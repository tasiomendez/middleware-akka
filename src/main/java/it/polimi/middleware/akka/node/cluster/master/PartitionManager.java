package it.polimi.middleware.akka.node.cluster.master;

import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.CreateRingMessage;
import it.polimi.middleware.akka.messages.join.IdRequestMessage;
import it.polimi.middleware.akka.messages.join.IdResponseMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionRequestMessage;
import it.polimi.middleware.akka.messages.storage.GetPartitionResponseMessage;
import it.polimi.middleware.akka.messages.update.NewSuccessorRequestMessage;
import it.polimi.middleware.akka.messages.update.NewSuccessorResponseMessage;
import it.polimi.middleware.akka.node.NodeID;

/**
 * The PartitionManager actor belongs to the master node. It is in charge of keeping track of 
 * the members in the cluster and their ids. It will provide new unique ids when needed.
 */
public class PartitionManager extends AbstractActor {

    private static final int PARTITION_NUMBER = (int) Math.pow(2, 32);
    private static final int REPLICATION_NUMBER = 2;

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Cluster cluster = Cluster.get(getContext().getSystem());

    private final AtomicInteger counter = new AtomicInteger(0);
    
    // The members tree stores id and actors, while the ids tree 
    // associates each address to an id.
    private final TreeMap<Integer, NodeID> members = new TreeMap<>();
    private final TreeMap<Integer, Integer> ids = new TreeMap<>();
    
    public PartitionManager() {
    	// Create ring at beginning
		getContext().getParent().tell(new CreateRingMessage(counter.get()), self());
		
		// Actor stored references ClusterManager
		final NodeID node = new NodeID(counter.get(), getContext().getParent());
		this.members.put(node.getId(), node);
		this.ids.put(getContext().self().path().address().hashCode(), node.getId());
	}

    private void onIdRequest(IdRequestMessage msg) {
        final int id = counter.incrementAndGet();
        log.info("Received id request from {}, assigning id {}", sender().path().address(), id);
        
        final NodeID entry = this.getRandomMember();
        log.debug("Assigning successor with id {}, path {}", entry.getId(), entry.getActor().path());
        sender().tell(new IdResponseMessage(id, entry), self());
        
        // Actor stored references ClusterManager
        final NodeID node = new NodeID(id, sender());
        this.members.put(node.getId(), node);
        this.ids.put(msg.getMember().address().hashCode(), node.getId());
    }
    
    private void onUnreachableMember(UnreachableMember msg) {
    	cluster.down(msg.member().address());
    	log.info("Member {} marked as down", msg.member().address());
    	final int id = this.ids.remove(msg.member().address().hashCode());
    	this.members.remove(id);
    }
    
    private void onNewSuccessorRequest(NewSuccessorRequestMessage msg) {
    	final NodeID entry = getRandomMember();
    	sender().tell(new NewSuccessorResponseMessage(entry), self());
    }
    
    private void onGetPartitionRequest(GetPartitionRequestMessage msg) {
    	final int key = msg.getEntry().getKey().hashCode() % PARTITION_NUMBER;
    	final NodeID partition = members.lowerEntry(key).getValue();
    	log.debug("Partition for key [{}] is [{}]", msg.getEntry().getKey(), partition.getActor().path());
    	partition.getActor().tell(new GetPartitionResponseMessage(msg.getEntry(), msg.getReplyTo()), self());
    }
    
    /**
     * Get random member from the TreeMap. 
     * 
     * @return the member
     */
    private NodeID getRandomMember() {
    	// Get random member
        final Object[] entries = this.members.values().toArray();
        final Random generator = new Random();
        return (NodeID) entries[generator.nextInt(entries.length)];
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(IdRequestMessage.class,  this::onIdRequest)
                .match(UnreachableMember.class, this::onUnreachableMember)
                
                .match(NewSuccessorRequestMessage.class, this::onNewSuccessorRequest)
                
                .match(GetPartitionRequestMessage.class, this::onGetPartitionRequest)

				.matchAny(msg -> log.warning("Received unknown message: {}", msg))
                .build();
    }

    public static Props props() {
        return Props.create(PartitionManager.class);
    }
}
