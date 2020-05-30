package it.polimi.middleware.akka.messages.join;

import akka.actor.ActorRef;

import java.io.Serializable;

public class MoveStorageMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ActorRef destination;
    private final int fromKey;
    private final int toKey;

    public MoveStorageMessage(ActorRef destination, int fromKey, int toKey) {
        this.destination = destination;
        this.fromKey = fromKey;
        this.toKey = toKey;
    }

    public ActorRef getDestination() {
        return destination;
    }

    public int getFromKey() {
        return fromKey;
    }

    public int getToKey() {
        return toKey;
    }
}
