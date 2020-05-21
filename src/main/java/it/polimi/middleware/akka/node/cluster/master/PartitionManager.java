package it.polimi.middleware.akka.node.cluster.master;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.CreateRingMessage;
import it.polimi.middleware.akka.messages.IdRequestMessage;
import it.polimi.middleware.akka.messages.IdResponseMessage;
import it.polimi.middleware.akka.node.Successor;

public class PartitionManager extends AbstractActor {

    private static final int PARTITION_NUMBER = (int) Math.pow(2, 32);
    private static final int REPLICATION_NUMBER = 2;

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Cluster cluster = Cluster.get(getContext().getSystem());

    private final AtomicInteger counter = new AtomicInteger(0);
    private final TreeMap<Integer, ActorRef> members = new TreeMap<>();
    
    public PartitionManager() {
    	// Create ring at beginning
		getContext().getParent().tell(new CreateRingMessage(counter.get()), self());
		members.put(counter.get(), getContext().getParent());
	}

    private void onIdRequest(IdRequestMessage msg) {
        final int id = counter.incrementAndGet();
        log.info("Received id request from {}, assigning id {}", sender().path(), id);

        Map.Entry<Integer, ActorRef> entry = members.lastEntry();
        
        log.debug("Assigning successor with id {}, path {}", entry.getKey(), entry.getValue().path());
        
        Successor successor = new Successor(entry.getKey(), entry.getValue());
        sender().tell(new IdResponseMessage(id, successor), self());
        
        members.put(id, sender());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(IdRequestMessage.class, this::onIdRequest)
                .build();
    }

    public static Props props() {
        return Props.create(PartitionManager.class);
    }
}
