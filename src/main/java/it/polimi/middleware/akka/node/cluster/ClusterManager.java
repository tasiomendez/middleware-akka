package it.polimi.middleware.akka.node.cluster;

import java.util.Map;
import java.util.TreeMap;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.MemberRemoved;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.CreateRingMessage;
import it.polimi.middleware.akka.messages.api.SuccessMessage;
import it.polimi.middleware.akka.messages.heartbeat.GetPredecessorRequestMessage;
import it.polimi.middleware.akka.messages.heartbeat.GetPredecessorResponseMessage;
import it.polimi.middleware.akka.messages.heartbeat.NotifyMessage;
import it.polimi.middleware.akka.messages.join.FindSuccessorRequestMessage;
import it.polimi.middleware.akka.messages.join.FindSuccessorResponseMessage;
import it.polimi.middleware.akka.messages.join.IdRequestMessage;
import it.polimi.middleware.akka.messages.join.IdResponseMessage;
import it.polimi.middleware.akka.messages.join.MasterNotificationMessage;
import it.polimi.middleware.akka.messages.storage.GathererMessage;
import it.polimi.middleware.akka.messages.storage.GathererStorageMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionGetterRequestMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionGetterResponseMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionRequestMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionResponseMessage;
import it.polimi.middleware.akka.messages.storage.PropagateMessage;
import it.polimi.middleware.akka.messages.storage.PropagateRequestMessage;
import it.polimi.middleware.akka.messages.storage.RestoreRequestMessage;
import it.polimi.middleware.akka.messages.update.NewSuccessorRequestMessage;
import it.polimi.middleware.akka.messages.update.NewSuccessorResponseMessage;
import it.polimi.middleware.akka.node.Reference;
import it.polimi.middleware.akka.node.cluster.master.PartitionManager;

/**
 * The ClusterManager actor is in charge of managing the cluster taking into account if a new node is joined, if a node
 * is removed from the cluster and all the changes within it. It is the one in charge of managing the stability between
 * the nodes.
 * <p>
 * It supervises the {@link ClusterListener} and in the master node it will supervises the {@link PartitionManager}.
 */
public class ClusterManager extends AbstractActor {

    private final int PARTITION_NUMBER = getContext().getSystem().settings().config().getInt("clustering.partition.max");
    private final int REPLICATION_NUMBER = getContext().getSystem().settings().config().getInt("clustering.replication");
    private final int FINGER_TABLE_SIZE = (int) (Math.log(this.PARTITION_NUMBER) / Math.log(2));

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Cluster cluster = Cluster.get(getContext().system());
    private final ActorRef listener = getContext().actorOf(ClusterListener.props(), "clusterListener");
    private final ActorRef partitionManager;

    private final HeartBeat heartbeat = HeartBeat.get(getContext().getSystem());

    private final Reference self = Reference.empty();
    private final Reference successor = Reference.empty();
    private final Reference predecessor = Reference.empty();

    private final TreeMap<Integer, Reference> fingerTable = new TreeMap<>();

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
     * @param lowerBound     lower limit
     * @param upperBound     upper limit
     * @param inclusiveLower include lower limit if true
     * @param inclusiveUpper include upper limit if true
     * @return true if is between, false otherwise
     */
    private static boolean isBetween(int id, int lowerBound, int upperBound, boolean inclusiveLower, boolean inclusiveUpper) {
        boolean checkLower = inclusiveLower ? id >= lowerBound : id > lowerBound;
        boolean checkUpper = inclusiveUpper ? id <= upperBound : id < upperBound;
        return (checkLower && checkUpper) || (upperBound <= lowerBound && (checkLower || checkUpper));
    }

    public static Props props() {
        return Props.create(ClusterManager.class);
    }
    
    public void initFingerTable() {
    	// Initialize finger table		
    	for (int i = 0; i < FINGER_TABLE_SIZE; i++) {
    		this.fingerTable.put((self.getId() + (int) Math.pow(2, i)) % PARTITION_NUMBER, this.self);
    	}
    }

    /**
     * Heartbeat functionality. When the successor is alive, ask for its predecessor in order to keep the stability of
     * the system.
     */
    private void heartbeat() {
        this.successor.getActor().tell(new GetPredecessorRequestMessage(), self());
        for (Integer key : fingerTable.keySet()) {
            self().tell(new FindSuccessorRequestMessage(key), self());
        }
        log.debug("Ring set as {} - {} - {}", this.predecessor, this.self, this.successor);
        log.debug("Finger table set as {}", fingerTable);
    }

    /**
     * Handles an incoming {@link CreateRingMessage}. Create the ring at the beginning. This action is performed by the
     * master node.
     *
     * @param msg create ring message.
     */
    private void onCreateRing(CreateRingMessage msg) {
        this.self.update(msg.getId(), self());
        log.info("Creating ring, set id to [{}]", this.self);
        this.successor.update(this.self);
        initFingerTable();
        this.heartbeat.start(this::heartbeat);
    }

    /**
     * Handles an incoming {@link MasterNotificationMessage} Get a notification of the master node at the beginning in
     * order to keep its actor reference.
     *
     * @param msg master notification message
     */
    private void onMasterNotification(MasterNotificationMessage msg) {
        log.debug("Master found at {}", msg.getMaster().path());
        this.master = msg.getMaster();
        this.master.tell(new IdRequestMessage(cluster.selfMember()), self());
    }

    /**
     * Handle an incoming {@link IdResponseMessage} by setting the id of the self node and by starting a search of its
     * successor.
     *
     * @param msg id response message
     */
    private void onIdResponse(IdResponseMessage msg) {
        this.self.update(msg.getId(), self());
        log.info("Set id to [{}], entry node is [{}]", this.self, msg.getSuccessor().getActor().path().address());
        initFingerTable();
        msg.getSuccessor().getActor().tell(new FindSuccessorRequestMessage(this.self.getId()), self());
    }

    /**
     * Handles an incoming {@link FindSuccessorRequestMessage}. If the sender of the message has an ID which is between
     * my id and my successor id, then the successor of the sender is my successor and replies with {@link
     * FindSuccessorResponseMessage}. If not, then forward the message to my successor in order to continue the search.
     *
     * @param msg find successor request message
     */
    private void onFindSuccessorRequest(FindSuccessorRequestMessage msg) {
//        log.debug("Received FindSuccessorRequestMessage (self={}, successor={}, requester={})",
//                this.self, this.successor, msg);

        if (isBetween(msg.getRequest(), this.self.getId(), this.successor.getId(), false, true)) {
            // reply directly to the request
//            log.debug("Successor found for requester={}", msg);
            sender().tell(new FindSuccessorResponseMessage(this.successor, msg.getRequest()), self());
        } else {
            // forward the message to the successor
//            log.debug("Successor not found for requester={}. Forwarding message to successor", msg);
            this.successor.getActor().forward(msg, getContext());
        }
    }

    /**
     * Handles an incoming {@link FindSuccessorResponseMessage} that occurs when the successor has been found. Then, the
     * successor reference is updated.
     *
     * @param msg
     */
    private void onFindSuccessorResponse(FindSuccessorResponseMessage msg) {
    	this.successor.update(msg.getResponse());
    	log.debug("Successor found as {}", this.successor);
    	log.info("Successor updated to {}", this.successor.getActor().path());
    	if (!this.heartbeat.started()) {
    		this.heartbeat.start(this::heartbeat);
    	}
    }
    
    /**
     * Handles an incoming {@link FindSuccessorResponseMessage} that occurs when the successor has been found when asking
     * for it on a Heartbeat message.
     * 
     * @param msg
     */
    private void onFindSuccessorFingerTableResponse(FindSuccessorResponseMessage msg) {
    	if (fingerTable.containsKey(msg.getIdRequest())) {
            fingerTable.put(msg.getIdRequest(), msg.getResponse());
        }
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
        // if the `predecessor of the successor` is between the node id and the successor id
        // then update the successor
        if (!msg.getPredecessor().isNull() &&
                isBetween(successorPredecessorId, this.self.getId(), this.successor.getId(), false, false)) {
            log.debug("Successor updated: [{}] -> [{}]", this.successor.getId(), successorPredecessorId);
            this.successor.update(msg.getPredecessor());
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
        log.debug("Received notification from predecessor {}", msg.getSender());
        if (this.predecessor.isNull() || isBetween(msg.getSender().getId(), this.predecessor.getId(), this.self.getId(), false, false)) {
            log.debug("Predecessor updated: [{}] -> [{}]",
                    this.predecessor.getId() == -1 ? "null" : this.predecessor.getId(), msg.getSender().getId());
            this.predecessor.update(msg.getSender());
            log.info("Predecessor updated to {}", this.predecessor.getActor().path().address());
        }
    }

    /**
     * Handles an {@link UnreachableMember} message. If the member detected as removed is my successor or
     * predecessor, a new search needs to be performed. The master node will forward the message to the {@link
     * PartitionManager} in order to mark it as down.
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
     * Handles a {@link NewSuccessorRequestMessage} when a new successor reference is required to the master node.
     *
     * @param msg new successor response message.
     */
    private void onNewSuccessorResponse(NewSuccessorResponseMessage msg) {
    	log.info("New successor provided from {}", sender());
    	this.successor.update(msg);
    }

    /**
     * Handles an {@link MemberRemoved} message. When the member is removed from the cluster, the data it stored
     * is replicated into a new node.
     * 
     * @param msg member removed
     */
    private void onMemberRemoved(MemberRemoved msg) {
    	this.heartbeat.execute(() -> {
    		// Restore keys from the unreachable member
    		getContext().getParent().tell(new RestoreRequestMessage(msg.member().address()), self());
    	}, 2);
    }

    private void onGetPartitionRequest(GetPartitionRequestMessage msg) {
        if (cluster.selfMember().hasRole("master")) {
            this.partitionManager.forward(msg, getContext());
        } else {
            this.master.forward(msg, getContext());
        }
    }

    private void onGetPartitionResponse(GetPartitionResponseMessage msg) {
        getContext().getParent().forward(msg, getContext());
        log.debug("Propagating PutterMessage to successor nodes");
        final PropagateRequestMessage message = new PropagateRequestMessage(msg.getEntry(), self(), REPLICATION_NUMBER - 1);
        successor.getActor().tell(message, self());
    }

    private void onPropagateRequest(PropagateRequestMessage msg) {
        if (msg.getOriginator().equals(self())) {
            log.warning("Detected loop while propagating PropagateRequestMessage, " +
                    "this is due to not having enough nodes in the cluster");
            return;
        }
        log.debug("Received PropagateRequestMessage from {} with hops to live {}", sender(), msg.getHopsToLive());
        getContext().getParent().tell(new PropagateMessage(msg.getEntry(), msg.getOriginator().path().address()), self());
        if (msg.mustPropagate()) {
            log.debug("Propagating PutterMessage to successor nodes", msg.getHopsToLive());
            successor.getActor().tell(msg.propagate(), self());
        }
    }
    
    /**
     * If the key is between two entries in the Finger Table, the message is forwarded to the lower entry,
     * and it is done recursively until the node with the key is reached. In case the key is not in the 
     * finger table, the message is forwarded to the farthest node.
     * 
     * @param msg
     */
    private void onGetPartitionGetterRequest(GetPartitionGetterRequestMessage msg) {
    	final int key = Math.abs(msg.getEntry().getKey().hashCode() % PARTITION_NUMBER);
    	if (isBetween(key, this.predecessor.getId(), this.self.getId(), false, true)) {
    		log.debug("Partition with key [{}] found on current node [{}]", msg.getEntry().getKey(), this.self);
    		getContext().getParent().tell(new GetPartitionGetterResponseMessage(msg.getEntry(), msg.getReplyTo()), self());
    		return;
    	}
    	
    	final Reference partition = this.getFloorReference(key);
    	log.debug("Received GetPartitionGetterRequestMessage from [{}] with hash [{}]", sender().path(), key);
    	if (partition.getId() == this.self.getId()) {
    		log.debug("Partition with key [{}] not found on finger table. Forwarding message.", msg.getEntry().getKey());
    		this.getFarthestReference().getActor().forward(msg, getContext());
    	} else {
    		log.debug("Partition with key [{}] found on finger table. Forwarding message to [{}].", msg.getEntry().getKey(), partition.getId());
    		partition.getActor().forward(msg, getContext());
    	}
    }

    /**
     * Searches the {@link PartitionManager#members} {@link TreeMap} for an entry whose key is lower or at most equal
     * to the given key. Normally the method {@link TreeMap#ceilingEntry(Object)} will return {@code null} if the
     * requested key is the biggest among the entries of the map, but in a circular ring this is not the expected
     * behaviour, so the search is repeated starting from the greatest key.
     *
     * @param key the {@link Integer} key to look for
     * @return an entry whose key is lower or at most equal to the given key
     */
    private Reference getFloorReference(int key) {
        final Map.Entry<Integer, Reference> entry = this.fingerTable.floorEntry(key);
        return (entry == null) ? this.fingerTable.floorEntry(this.fingerTable.lastKey()).getValue() : entry.getValue();
    }
    
    private Reference getFarthestReference() {
        final Map.Entry<Integer, Reference> entry = this.fingerTable.lowerEntry(this.self.getId());
        return (entry == null) ? this.fingerTable.lastEntry().getValue() : entry.getValue();
    }
    
    private Reference higherEntry(int key) {
        final Map.Entry<Integer, Reference> entry = this.fingerTable.higherEntry(key);
        return (entry == null) ? this.fingerTable.firstEntry().getValue() : entry.getValue();
    }
    
    private void onGathererStorage(GathererStorageMessage msg) {
    	log.debug("Forwarding GathererMessage to successor");
    	final Reference originator = (msg.getOriginator() != null) ? msg.getOriginator() : this.self;
    	final GathererMessage gatherer = new GathererMessage(msg.getAccumulator(), originator, msg.getReplyTo());
    	this.successor.getActor().tell(gatherer, self());
    }
    
    private void onGatherer(GathererMessage msg) {
    	if (msg.getOriginator().equals(this.self)) {
    		log.debug("Gatherer loop finished. Sending http response");
    		msg.getReplyTo().tell(new SuccessMessage(msg.getAccumulator()), self());
    	} else {
	    	log.debug("Gatherer message recived with originator [{}]", msg.getOriginator().getId());
	    	getContext().getParent().tell(new GathererStorageMessage(msg), self());
    	}
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

                .match(FindSuccessorRequestMessage.class, this::onFindSuccessorRequest)
                .match(FindSuccessorResponseMessage.class, 
                		(msg) -> msg.getIdRequest() == this.self.getId(), // Current node asks for its successor
                		this::onFindSuccessorResponse)
                .match(FindSuccessorResponseMessage.class, 
                		this::onFindSuccessorFingerTableResponse) // Answer to a FingerTable Heartbeat message

                // HeartBeat messages
                .match(GetPredecessorRequestMessage.class, this::onGetPredecessorRequest)
                .match(GetPredecessorResponseMessage.class, this::onGetPredecessorResponse)

                // HeartBeat messages - Notifications
                .match(NotifyMessage.class, this::onNotify)

                .match(UnreachableMember.class, this::onUnreachableMember)
                .match(NewSuccessorRequestMessage.class,
                        () -> cluster.selfMember().hasRole("master"),
                        (msg) -> partitionManager.forward(msg, getContext()))
                .match(NewSuccessorResponseMessage.class, this::onNewSuccessorResponse)
                .match(MemberRemoved.class, this::onMemberRemoved)

                // Storage messages
                .match(GetPartitionRequestMessage.class, this::onGetPartitionRequest)
                .match(GetPartitionResponseMessage.class, this::onGetPartitionResponse)
                .match(PropagateRequestMessage.class, this::onPropagateRequest)
                .match(GetPartitionGetterRequestMessage.class, this::onGetPartitionGetterRequest)
                .match(GetPartitionGetterResponseMessage.class, (msg) -> getContext().getParent().forward(msg, getContext()))
                .match(GathererStorageMessage.class, this::onGathererStorage)
                .match(GathererMessage.class, this::onGatherer)

                .matchAny(msg -> log.warning("Received unknown message: {}", msg))
                .build();
    }

    @Override
    public String toString() {
        return this.self.toString();
    }
}
