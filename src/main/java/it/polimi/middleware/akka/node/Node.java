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
import it.polimi.middleware.akka.messages.PutterMessage;
import it.polimi.middleware.akka.node.cluster.ClusterManager;
import it.polimi.middleware.akka.node.storage.Storage;

public class Node extends AbstractActor {

    private static final int PARTITION_NUMBER = (int) Math.pow(2, 32);
    private static final int REPLICATION_NUMBER = 2;

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Cluster cluster = Cluster.get(getContext().getSystem());
    private final ActorRef clusterManager = getContext().actorOf(ClusterManager.props(), "clusterManager");
    private final ActorRef storage = getContext().actorOf(Storage.props(), "storage");

    private int id;
    private ActorRef successor = null;
    private int successorId;
    private ActorRef predecessor = null;
    private int predecessorId;

    public static Props props() {
        return Props.create(Node.class);
    }

    private void onCreateRing(CreateRingMessage msg) {
        id = msg.getId();
        log.info("Creating ring, set id to {}", id);
        successor = self();
        successorId = id;
    }

    private void onIdResponse(IdResponseMessage msg) {
        id = msg.getId();
        log.info("Set id to {}, entry node is {}", id, msg.getSuccessor().path());
        msg.getSuccessor().tell(new FindSuccessorRequestMessage(id), self());
    }

    private void onFindSuccessorRequest(FindSuccessorRequestMessage msg) {
        log.debug("Received FindSuccessorRequestMessage (self={}, successor={}, requester={})",
                id, successorId, msg.getId());

        if ((successorId >= id && msg.getId() > id && msg.getId() <= successorId)
                || (successorId <= id && (msg.getId() > id || msg.getId() <= successorId))) {
            // reply directly to the request
            log.debug("Replying");
            sender().tell(new FindSuccessorResponseMessage(successor, successorId), self());
        } else {
            // forward the message to the successor
            log.debug("Forwarding to successor");
            successor.forward(msg, getContext());
        }
    }

    private void onFindSuccessorResponse(FindSuccessorResponseMessage msg) {
        successor = msg.getSuccessor();
        successorId = msg.getSuccessorId();
        log.debug("Set successor to {}", successorId);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GetterMessage.class, msg -> storage.forward(msg, getContext()))
                .match(PutterMessage.class, msg -> storage.forward(msg, getContext()))
                .match(CreateRingMessage.class, this::onCreateRing)
                .match(IdResponseMessage.class, this::onIdResponse)
                .match(FindSuccessorRequestMessage.class, this::onFindSuccessorRequest)
                .match(FindSuccessorResponseMessage.class, this::onFindSuccessorResponse)
                .matchAny(msg -> log.warning("Received unknown message: {}", msg))
                .build();
    }

}
