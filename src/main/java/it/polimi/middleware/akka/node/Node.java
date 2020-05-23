package it.polimi.middleware.akka.node;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.CreateRingMessage;
import it.polimi.middleware.akka.messages.FindSuccessorRequestMessage;
import it.polimi.middleware.akka.messages.FindSuccessorResponseMessage;
import it.polimi.middleware.akka.messages.GetterMessage;
import it.polimi.middleware.akka.messages.IdResponseMessage;
import it.polimi.middleware.akka.messages.PropagatePutterMessage;
import it.polimi.middleware.akka.messages.PutterMessage;
import it.polimi.middleware.akka.messages.heartbeat.GetPredecessorRequestMessage;
import it.polimi.middleware.akka.messages.heartbeat.GetPredecessorResponseMessage;
import it.polimi.middleware.akka.messages.heartbeat.NotifyMessage;
import it.polimi.middleware.akka.node.cluster.ClusterManager;
import it.polimi.middleware.akka.node.storage.Storage;

import java.time.Duration;

public class Node extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Cluster cluster = Cluster.get(getContext().getSystem());
    private final ActorRef clusterManager = getContext().actorOf(ClusterManager.props(), "clusterManager");
    private final ActorRef storage = getContext().actorOf(Storage.props(), "storage");

    private final Reference successor = Reference.empty();
    private final Reference predecessor = Reference.empty();

    private ActorRef master;
    private int id;

    public static Props props() {
        return Props.create(Node.class);
    }

    private static boolean isBetween(int id, int lowerBound, int upperBound, boolean inclusiveLower, boolean inclusiveUpper) {
        boolean checkLower = inclusiveLower ? id >= lowerBound : id > lowerBound;
        boolean checkUpper = inclusiveUpper ? id <= upperBound : id < upperBound;
        return (checkLower && checkUpper) || (upperBound <= lowerBound && (checkLower || checkUpper));
    }

    private void initHeartBeat() {
        log.debug("Starting heartbeat");
        getContext().getSystem().scheduler().schedule(
                Duration.ofSeconds(4), // initial delay
                Duration.ofSeconds(4), // delay between each invocation
                this::heartbeat,
                getContext().getSystem().dispatcher());
    }

    private void heartbeat() {
        if (successor.getActorRef() == null) {
            log.debug("Successor not set yet");
            return;
        }
        successor.getActorRef().tell(new GetPredecessorRequestMessage(), self());
    }

    private void onPropagatePutter(PropagatePutterMessage msg) {
        if (msg.isPropagated() && msg.getInitiator().equals(self())) {
            log.warning("Detected loop while propagating insertion of key-value pair, " +
                    "this is due to not having enough nodes in the cluster");
            return;
        }
        if (msg.isPropagated()) {
            storage.tell(msg.getMsg(), self());
        } else {
            storage.forward(msg.getMsg(), getContext());
        }
        if (msg.getHopsToLive() <= 1) {
            log.debug("Hops to live reached 0, not forwarding to successor");
            return;
        }
        successor.getActorRef().forward(msg.propagate(), getContext());
    }

    private void onCreateRing(CreateRingMessage msg) {
        master = sender();
        id = msg.getId();
        log.info("Creating ring, set id to {}", id);
        successor.update(id, self());
        initHeartBeat();
    }

    private void onIdResponse(IdResponseMessage msg) {
        master = sender();
        id = msg.getId();
        log.info("Set id to {}, entry node is {}", id, msg.getSuccessor().getActorRef().path());
        msg.getSuccessor().getActorRef().tell(new FindSuccessorRequestMessage(id), self());
        initHeartBeat();
    }

    private void onFindSuccessorRequest(FindSuccessorRequestMessage msg) {
        log.debug("Received FindSuccessorRequestMessage (self={}, successor={}, requester={})",
                id, successor, msg.getId());

        if (isBetween(msg.getId(), id, successor.getId(), false, true)) {
            // reply directly to the request
            log.debug("Replying");
            sender().tell(new FindSuccessorResponseMessage(successor), self());
        } else {
            // forward the message to the successor
            log.debug("Forwarding to successor");
            successor.getActorRef().forward(msg, getContext());
        }
    }

    private void onFindSuccessorResponse(FindSuccessorResponseMessage msg) {
        successor.update(msg);
        log.debug("Set successor to {}", successor);
    }

    /**
     * Respond to a {@link GetPredecessorRequestMessage} by sending the reference to the node's predecessor and its id.
     *
     * @param msg the predecessor request message
     */
    private void onGetPredecessorRequest(GetPredecessorRequestMessage msg) {
        sender().tell(new GetPredecessorResponseMessage(predecessor), self());
    }

    /**
     * Handles an incoming {@link GetPredecessorResponseMessage}, which is in response to a {@link
     * GetPredecessorRequestMessage}. This method updates the successor in case a new node has joined the circle with an
     * id greater than this node's id, but smaller then the current successor's id, which means it is closer to this
     * node then the current successor.
     *
     * @param msg the predecessor response message
     */
    private void onGetPredecessorResponse(GetPredecessorResponseMessage msg) {
        // get the predecessor id from the message
        int successorPredecessorId = msg.getId();
        // if the `predecessor of the successor` is between the node id and the successor id then update the
        // successor
        if (msg.getActorRef() != null &&
                isBetween(successorPredecessorId, id, successor.getId(), false, false)) {
            log.debug("Updating successor: {} -> {}", successor, msg);
            successor.update(msg);
        }
        // notify the successor of the presence of this node
        log.debug("Sending notification to {}", successor);
        successor.getActorRef().tell(new NotifyMessage(new Reference(id, self())), self());
    }

    /**
     * Handles an incoming {@link NotifyMessage} from a potential predecessor, so that this node can update its
     * predecessor reference.
     *
     * @param msg thr notification message
     */
    private void onNotify(NotifyMessage msg) {
        log.debug("Received notification from {}", msg.getId());
        if (predecessor.getActorRef() == null || isBetween(msg.getId(), predecessor.getId(), id, false, false)) {
            log.debug("Updating predecessor: {} -> {}", predecessor, msg);
            predecessor.update(msg);
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GetterMessage.class, msg -> storage.forward(msg, getContext()))
                .match(PutterMessage.class, msg -> master.forward(msg, getContext()))
                .match(PropagatePutterMessage.class, this::onPropagatePutter)
                .match(CreateRingMessage.class, this::onCreateRing)
                .match(IdResponseMessage.class, this::onIdResponse)
                .match(FindSuccessorRequestMessage.class, this::onFindSuccessorRequest)
                .match(FindSuccessorResponseMessage.class, this::onFindSuccessorResponse)
                // heartbeat messages
                .match(GetPredecessorRequestMessage.class, this::onGetPredecessorRequest)
                .match(GetPredecessorResponseMessage.class, this::onGetPredecessorResponse)
                .match(NotifyMessage.class, this::onNotify)
                .matchAny(msg -> log.warning("Received unknown message: {}", msg))
                .build();
    }

}
