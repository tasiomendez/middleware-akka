package it.polimi.middleware.akka.node.cluster;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.CreateRingMessage;
import it.polimi.middleware.akka.messages.heartbeat.GetPredecessorRequestMessage;
import it.polimi.middleware.akka.messages.heartbeat.GetPredecessorResponseMessage;
import it.polimi.middleware.akka.messages.heartbeat.NotifyMessage;
import it.polimi.middleware.akka.messages.join.FindSuccessorRequestMessage;
import it.polimi.middleware.akka.messages.join.FindSuccessorResponseMessage;
import it.polimi.middleware.akka.messages.join.IdRequestMessage;
import it.polimi.middleware.akka.messages.join.IdResponseMessage;
import it.polimi.middleware.akka.messages.join.MasterNotificationMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionRequestMessage;
import it.polimi.middleware.akka.messages.update.NewSuccessorRequestMessage;
import it.polimi.middleware.akka.messages.update.NewSuccessorResponseMessage;
import it.polimi.middleware.akka.node.Reference;
import it.polimi.middleware.akka.node.cluster.master.PartitionManager;

/**
 * The ClusterManager actor is in charge of managing the cluster taking into account if a new node
 * is joined, if a node is removed from the cluster and all the changes within it. It is the one in 
 * charge of managing the stability between the nodes.
 * 
 * It supervises the {@link ClusterListener} and in the master node it will supervises the {@link PartitionManager}.
 */
public class ClusterManager extends AbstractActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	private final Cluster cluster = Cluster.get(getContext().system());
	private final ActorRef listener = getContext().actorOf(ClusterListener.props(), "clusterListener");
	private final ActorRef partitionManager;

	private final HeartBeat heartbeat = HeartBeat.get(getContext().getSystem());

	private final Reference self = Reference.empty();
	private final Reference successor = Reference.empty();
	private final Reference predecessor = Reference.empty();
	
	// Actor Reference to Master ClusterManager
	// Set as default to self. It will be updated to the real one after joining the cluster
	private ActorRef master = getContext().self();

	public ClusterManager() {
		this.partitionManager = (cluster.selfMember().hasRole("master")) ?
				getContext().actorOf(PartitionManager.props(), "partitionManager") : null;
	}

	/**
	 * Check if the id passed as parameter is between the bounds.
	 * 
	 * @param id
	 * @param lowerBound lower limit
	 * @param upperBound upper limit
	 * @param inclusiveLower include lower limit if true
	 * @param inclusiveUpper include upper limit if true
	 * @return true if is between, false otherwise
	 */
	private static boolean isBetween(int id, int lowerBound, int upperBound, boolean inclusiveLower, boolean inclusiveUpper) {
		boolean checkLower = inclusiveLower ? id >= lowerBound : id > lowerBound;
		boolean checkUpper = inclusiveUpper ? id <= upperBound : id < upperBound;
		return (checkLower && checkUpper) || (upperBound <= lowerBound && (checkLower || checkUpper));
	}
	
	/**
	 * Heartbeat functionality. When the successor is alive, ask for its predecessor in order
	 * to keep the stability of the system.
	 */
	private void heartbeat() {
		this.successor.getActor().tell(new GetPredecessorRequestMessage(), self());
		log.debug("Ring set as {} - {} - {}", this.predecessor, this.self, this.successor);
	}
	
	/**
	 * Handles an incoming {@link CreateRingMessage}. Create the ring at the beginning. 
	 * This action is performed by the master node.
	 * 
	 * @param msg create ring message.
	 */
	private void onCreateRing(CreateRingMessage msg) {
		this.self.update(msg.getId(), self());
		log.info("Creating ring, set id to [{}]", this.self);
		this.successor.update(this.self);
		this.heartbeat.start(this::heartbeat);
	}
	
	/**
	 * Handles an incoming {@link MasterNotificationMessage} Get a notification of 
	 * the master node at the beginning in order to keep its actor reference.
	 * 
	 * @param msg master notification message
	 */
	private void onMasterNotification(MasterNotificationMessage msg) {
		log.debug("Master found at {}", msg.getMaster().path());
		this.master = msg.getMaster();
		this.master.tell(new IdRequestMessage(cluster.selfMember()), self());
	}

	/**
	 * Handle an incoming {@link IdResponseMessage} by setting the id of the
	 * self node and by starting a search of its successor.
	 * 
	 * @param msg id response message
	 */
	private void onIdResponse(IdResponseMessage msg) {
		this.self.update(msg.getId(), self());
		log.info("Set id to [{}], entry node is [{}]", this.self, msg.getSuccessor().getActor().path().address());
		msg.getSuccessor().getActor().tell(new FindSuccessorRequestMessage(this.self), self());
	}

	/**
	 * Handles an incoming {@link FindSuccessorRequestMessage}. If the sender of the message
	 * has an ID which is between my id and my successor id, then the successor of the sender
	 * is my successor and replies with {@link FindSuccessorResponseMessage}.
	 * If not, then forward the message to my successor in order to continue the search.
	 * 
	 * @param msg find successor request message
	 */
	private void onFindSuccessorRequest(FindSuccessorRequestMessage msg) {
		log.debug("Received FindSuccessorRequestMessage (self={}, successor={}, requester={})",
				this.self, this.successor, msg);

		if (isBetween(msg.getId(), this.self.getId(), this.successor.getId(), false, true)) {
			// reply directly to the request
			log.debug("Successor found for requester={}", msg);
			sender().tell(new FindSuccessorResponseMessage(this.successor), self());
		} else {
			// forward the message to the successor
			log.debug("Successor not found for requester={}. Forwarding message to successor", msg);
			this.successor.getActor().forward(msg, getContext());
		}
	}
	
	/**
	 * Handles an incoming {@link FindSuccessorResponseMessage} that occurs when the successor
	 * has been found. Then, the successor reference is updated.
	 * 
	 * @param msg
	 */
	private void onFindSuccessorResponse(FindSuccessorResponseMessage msg) {
		this.successor.update(msg);
		log.debug("Successor found as {}", this.successor);
		log.info("Successor updated to {}", this.successor.getActor().path().address());
		this.heartbeat.start(this::heartbeat);
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
		int successorPredecessorId = msg.getId();
		// if the `predecessor of the successor` is between the node id and the successor id 
		// then update the successor
		if (!msg.isNull() &&
				isBetween(successorPredecessorId, this.self.getId(), this.successor.getId(), false, false)) {
			log.debug("Successor updated: [{}] -> [{}]", this.successor.getId(), successorPredecessorId);
			this.successor.update(msg);
			log.info("Successor updated to {}", this.successor.getActor().path().address());
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
		log.debug("Received notification from predecessor {}", msg);
		if (this.predecessor.isNull() || isBetween(msg.getId(), this.predecessor.getId(), this.self.getId(), false, false)) {
			log.debug("Predecessor updated: [{}] -> [{}]", 
					this.predecessor.getId() == -1 ? "null" : this.predecessor.getId(), msg.getId());
			this.predecessor.update(msg);
			log.info("Predecessor updated to {}", this.predecessor.getActor().path().address());
		}
	}
	
	/**
	 * Handles an {@link UnreachableMember} message. If the member detected as unreachable
	 * is my successor or predecessor, a new search needs to be performed.
	 * The master node will forward the message to the {@link PartitionManager} in order
	 * to mark it as down.
	 * 
	 * @param msg unreachable member
	 */
	private void onUnreachableMember(UnreachableMember msg) {
		if (cluster.selfMember().hasRole("master"))
			partitionManager.forward(msg, getContext());
		
		if (this.successor.getActor().path().address().equals(msg.member().address())) {
			log.info("Successor detected as unreachable. Trying to find the new one");
			this.master.tell(new NewSuccessorRequestMessage(this.self), self());
		}
			
		if (this.predecessor.getActor().path().address().equals(msg.member().address())) {
			log.info("Predecessor detected as unreachable. Trying to find the new one");
			this.predecessor.update(Reference.empty());
		}
			
	}
	
	/**
	 * Handles a {@link NewSuccessorRequestMessage} when a new successor reference is 
	 * required to the master node.
	 * 
	 * @param msg new successor response message.
	 */
	private void onNewSuccessorResponse(NewSuccessorResponseMessage msg) {
		log.info("New successor provided from {}", sender());
		this.successor.update(msg);
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(CreateRingMessage.class, this::onCreateRing)
				
				.match(MasterNotificationMessage.class, this::onMasterNotification)

				.match(IdRequestMessage.class, 
						() -> cluster.selfMember().hasRole("master"), 
						(msg) -> partitionManager.forward(msg, getContext()))
				.match(IdResponseMessage.class, this::onIdResponse)

				.match(FindSuccessorRequestMessage.class,  this::onFindSuccessorRequest)
				.match(FindSuccessorResponseMessage.class, this::onFindSuccessorResponse)

				// HeartBeat messages
				.match(GetPredecessorRequestMessage.class,  this::onGetPredecessorRequest)
				.match(GetPredecessorResponseMessage.class, this::onGetPredecessorResponse)

				// HeartBeat messages - Notifications
				.match(NotifyMessage.class, this::onNotify)
				
				.match(UnreachableMember.class, this::onUnreachableMember)
				.match(NewSuccessorRequestMessage.class, 
						() -> cluster.selfMember().hasRole("master"), 
						(msg) -> partitionManager.forward(msg, getContext()))
				.match(NewSuccessorResponseMessage.class, this::onNewSuccessorResponse)
				
				// Storage messages
				.match(GetPartitionRequestMessage.class, 
						() -> cluster.selfMember().hasRole("master"), 
						(msg) -> partitionManager.forward(msg, getContext()))

				.matchAny(msg -> log.warning("Received unknown message: {}", msg))
				.build();
	}

	public static Props props() {
		return Props.create(ClusterManager.class);
	}

}
