package it.polimi.middleware.akka.master.partitionmanager;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.messages.CreateRingMessage;
import it.polimi.middleware.akka.messages.IdRequestMessage;
import it.polimi.middleware.akka.messages.IdResponseMessage;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PartitionManager extends AbstractActor {

    private static final int PARTITION_NUMBER = (int) Math.pow(2, 32);
    private static final int REPLICATION_NUMBER = 2;

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Cluster cluster = Cluster.get(getContext().getSystem());

    private final AtomicInteger idCounter = new AtomicInteger(0);
    private final TreeMap<Integer, ActorRef> members = new TreeMap<>();

    public static Props props() {
        return Props.create(PartitionManager.class);
    }

    private void onIdRequest(IdRequestMessage msg) {
        int id = idCounter.getAndIncrement();
        log.info("Received id request from {}, assigning id {}", sender().path(), id);

        if (id == 0) {
            log.debug("Creating ring");
            sender().tell(new CreateRingMessage(id), self());
        } else {
            Map.Entry<Integer, ActorRef> entry = members.lastEntry();
            log.debug("Sending successor with id {}, path {}", entry.getKey(), entry.getValue().path());
            sender().tell(new IdResponseMessage(id, entry.getValue(), entry.getKey()), self());
        }

        members.put(id, sender());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(IdRequestMessage.class, this::onIdRequest)
                .build();
    }
}
