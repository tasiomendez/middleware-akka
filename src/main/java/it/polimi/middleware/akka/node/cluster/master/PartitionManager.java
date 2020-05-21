package it.polimi.middleware.akka.node.cluster.master;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.UnreachableMember;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.CreateRingMessage;
import it.polimi.middleware.akka.messages.IdRequestMessage;
import it.polimi.middleware.akka.messages.IdResponseMessage;
import it.polimi.middleware.akka.node.NodeDef;

public class PartitionManager extends AbstractActor {

    private static final int PARTITION_NUMBER = (int) Math.pow(2, 32);
    private static final int REPLICATION_NUMBER = 2;

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Cluster cluster = Cluster.get(getContext().getSystem());

    private final AtomicInteger counter = new AtomicInteger(0);
    
    // The key for the TreeMap is the hash of the address of the node
    // in order to be able to search when node is marked as unreachable
    private final TreeMap<Integer, NodeDef> members = new TreeMap<>();
    
    public PartitionManager() {
    	// Create ring at beginning
		getContext().getParent().tell(new CreateRingMessage(counter.get()), self());
		final NodeDef node = new NodeDef(counter.get(), getContext().getParent());
		members.put(getContext().self().path().address().hashCode(), node);
	}

    private void onIdRequest(IdRequestMessage msg) {
        final int id = counter.incrementAndGet();
        log.info("Received id request from {}, assigning id {}", sender().path().address(), id);

        Map.Entry<Integer, NodeDef> entry = members.lastEntry();
        
        log.debug("Assigning successor with id {}, path {}", entry.getKey(), entry.getValue().getActor().path());
        sender().tell(new IdResponseMessage(id, entry.getValue()), self());
        
        final NodeDef node = new NodeDef(id, sender());
        this.members.put(msg.getMember().address().hashCode(), node);
    }
    
    private void onUnreachableMember(UnreachableMember msg) {
    	cluster.down(msg.member().address());
    	log.info("Member {} marked as down", msg.member().address());
    	this.members.remove(msg.member().address().hashCode());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(IdRequestMessage.class, this::onIdRequest)
                .match(UnreachableMember.class, this::onUnreachableMember)
                .build();
    }

    public static Props props() {
        return Props.create(PartitionManager.class);
    }
}
