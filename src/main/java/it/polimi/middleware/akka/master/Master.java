package it.polimi.middleware.akka.master;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import it.polimi.middleware.akka.master.partitionmanager.PartitionManager;
import it.polimi.middleware.akka.messages.IdRequestMessage;

public class Master extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final ActorRef partitionManager = getContext().getSystem().actorOf(PartitionManager.props(), "partitionManager");

    public static Props props() {
        return Props.create(Master.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(IdRequestMessage.class, msg -> partitionManager.forward(msg, context()))
                .matchAny(msg -> {
                })
                .build();
    }
}
