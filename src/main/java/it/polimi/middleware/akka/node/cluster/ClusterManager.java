package it.polimi.middleware.akka.node.cluster;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.CreateRingMessage;
import it.polimi.middleware.akka.messages.FindSuccessorRequestMessage;
import it.polimi.middleware.akka.messages.FindSuccessorResponseMessage;
import it.polimi.middleware.akka.messages.IdRequestMessage;
import it.polimi.middleware.akka.messages.IdResponseMessage;
import it.polimi.middleware.akka.messages.heartbeat.GetPredecessorRequestMessage;
import it.polimi.middleware.akka.messages.heartbeat.GetPredecessorResponseMessage;
import it.polimi.middleware.akka.messages.heartbeat.NotifyMessage;
import it.polimi.middleware.akka.node.NodeDef;
import it.polimi.middleware.akka.node.cluster.master.PartitionManager;

public class ClusterManager extends AbstractActor {

	private static final int PARTITION_NUMBER = (int) Math.pow(2, 32);
	private static final int REPLICATION_NUMBER = 2;

	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	private final Cluster cluster = Cluster.get(getContext().system());
	private final ActorRef listener = getContext().actorOf(ClusterListener.props(), "clusterListener");
	private final ActorRef master;

	private final HeartBeat heartbeat = HeartBeat.get(getContext().getSystem());

	private NodeDef self;
	private NodeDef successor = new NodeDef(-1, null);
	private NodeDef predecessor = new NodeDef(-1, null);

	public ClusterManager() {
		this.master = (cluster.selfMember().hasRole("master")) ?
				getContext().actorOf(PartitionManager.props(), "partitionManager") : null;
	}

	private static boolean isBetween(int id, int lowerBound, int upperBound, boolean inclusiveLower, boolean inclusiveUpper) {
		boolean checkLower = inclusiveLower ? id >= lowerBound : id > lowerBound;
		boolean checkUpper = inclusiveUpper ? id <= upperBound : id < upperBound;
		return (checkLower && checkUpper) || (upperBound <= lowerBound && (checkLower || checkUpper));
	}

	private void heartbeat() {
		if (this.successor.isNull()) {
			log.debug("Successor not set yet");
			return;
		}
		this.successor.getActor().tell(new GetPredecessorRequestMessage(), self());
		log.debug("Ring set as {} - {} - {}", this.predecessor, this.self, this.successor);
	}

	private void onCreateRing(CreateRingMessage msg) {
		this.self = new NodeDef(msg.getId(), self());
		log.info("Creating ring, set id to [{}]", this.self);
		this.successor = new NodeDef(this.self.getId(), self());
		this.heartbeat.start(this::heartbeat);
	}

	private void onIdResponse(IdResponseMessage msg) {
		this.self = new NodeDef(msg.getId(), self());
		log.info("Set id to [{}], entry node is [{}]", this.self, msg.getSuccessor().getActor().path());
		msg.getSuccessor().getActor().tell(new FindSuccessorRequestMessage(this.self), self());
		this.heartbeat.start(this::heartbeat);
	}

	private void onFindSuccessorRequest(FindSuccessorRequestMessage msg) {
		log.debug("Received FindSuccessorRequestMessage (self={}, successor={}, requester={})",
				this.self, this.successor.getId(), msg.getSender().getId());

		if (isBetween(msg.getSender().getId(), this.self.getId(), this.successor.getId(), false, true)) {
			// reply directly to the request
			log.debug("Successor found for requester={}", msg.getSender().getId());
			sender().tell(new FindSuccessorResponseMessage(this.successor), self());
		} else {
			// forward the message to the successor
			log.debug("Successor not found for requester={}. Forwarding message to successor", msg.getSender().getId());
			this.successor.getActor().forward(msg, getContext());
		}
	}

	private void onFindSuccessorResponse(FindSuccessorResponseMessage msg) {
		this.successor = msg.getSuccessor();
		log.debug("Successor found as {}", this.successor);
		log.info("Successor updated to {}", this.successor.getActor().path());
	}

	/**
	 * Respond to a {@link GetPredecessorRequestMessage} by sending the reference to the node's predecessor.
	 *
	 * @param msg predecessor request message
	 */
	private void onGetPredecessorRequest(GetPredecessorRequestMessage msg) {
		sender().tell(new GetPredecessorResponseMessage(this.predecessor), self());
	}

	/**
	 * Handles an incoming {@link GetPredecessorResponseMessage}, which is in response to a {@link
	 * GetPredecessorRequestMessage}. This method updates the successor in case a new node has joined the circle with an
	 * id greater than this node's id, but smaller then the current successor's id, which means it is closer to this
	 * node then the current successor.
	 *
	 * @param msg predecessor response message
	 */
	private void onGetPredecessorResponse(GetPredecessorResponseMessage msg) {
		int successorPredecessorId = msg.getPredecessor().getId();
		// if the `predecessor of the successor` is between the node id and the successor id then update the
		// successor
		if (!msg.getPredecessor().isNull() &&
				isBetween(successorPredecessorId, this.self.getId(), this.successor.getId(), false, false)) {
			log.debug("Successor updated: [{}] -> [{}]", this.successor.getId(), successorPredecessorId);
			this.successor = new NodeDef(msg.getPredecessor().getId(), msg.getPredecessor().getActor());
			log.info("Successor updated to {}", this.successor.getActor().path());
		}
		// notify the successor of the presence of this node
		log.debug("Sending notification to successor {}", this.successor);
		this.successor.getActor().tell(new NotifyMessage(this.self), self());
	}

	/**
	 * Handles an incoming {@link NotifyMessage} from a potential predecessor, so that this node can update its
	 * predecessor reference.
	 *
	 * @param msg notification message
	 */
	private void onNotify(NotifyMessage msg) {
		log.debug("Received notification from predecessor {}", msg.getSender());
		if (this.predecessor.isNull() || isBetween(msg.getSender().getId(), this.predecessor.getId(), this.self.getId(), false, false)) {
			log.debug("Predecessor updated: [{}] -> [{}]", 
					this.predecessor.getId() == -1 ? "null" : this.predecessor.getId(), msg.getSender().getId());
			this.predecessor = new NodeDef(msg.getSender().getId(), sender());
			log.info("Predecessor updated to {}", this.predecessor.getActor().path());
		}
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(CreateRingMessage.class, this::onCreateRing)

				.match(IdRequestMessage.class, () -> cluster.selfMember().hasRole("master"), msg -> master.forward(msg, getContext()))
				.match(IdResponseMessage.class, this::onIdResponse)

				.match(FindSuccessorRequestMessage.class,  this::onFindSuccessorRequest)
				.match(FindSuccessorResponseMessage.class, this::onFindSuccessorResponse)

				// HeartBeat messages
				.match(GetPredecessorRequestMessage.class, this::onGetPredecessorRequest)
				.match(GetPredecessorResponseMessage.class, this::onGetPredecessorResponse)

				// HeartBeat messages - Notifications
				.match(NotifyMessage.class, this::onNotify)

				.matchAny(msg -> log.warning("Received unknown message: {}", msg))
				.build();
	}

	public static Props props() {
		return Props.create(ClusterManager.class);
	}

}
