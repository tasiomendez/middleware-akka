package it.polimi.middleware.akka.messages.storage;

import java.io.Serializable;

import akka.actor.ActorRef;

public class PropagateRequestMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PutterMessage entry;
    private final ActorRef originator;

    private int hopsToLive;

    public PropagateRequestMessage(PutterMessage entry, ActorRef originator, int hopsToLive) {
        this.entry = entry;
        this.originator = originator;
        this.hopsToLive = hopsToLive;
    }

    public final PutterMessage getEntry() {
        return entry;
    }

    public ActorRef getOriginator() {
        return originator;
    }

    public int getHopsToLive() {
        return hopsToLive;
    }

    public boolean mustPropagate() {
        return hopsToLive > 1;
    }

    public PropagateRequestMessage propagate() {
        this.hopsToLive -= 1;
        return this;
    }
}
