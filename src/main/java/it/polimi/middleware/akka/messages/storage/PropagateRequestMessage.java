package it.polimi.middleware.akka.messages.storage;

import java.io.Serializable;
import java.util.HashMap;

import akka.actor.ActorRef;

public class PropagateRequestMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final HashMap<String, String> backup = new HashMap<>();
    private final ActorRef originator;

    private int hopsToLive;

    public PropagateRequestMessage(HashMap<String, String> backup, ActorRef originator, int hopsToLive) {
        this.backup.putAll(backup);
        this.originator = originator;
        this.hopsToLive = hopsToLive;
    }

    public PropagateRequestMessage(PutterMessage backup, ActorRef originator, int hopsToLive) {
        this.backup.put(backup.getKey(), backup.getValue());
        this.originator = originator;
        this.hopsToLive = hopsToLive;
    }

    public final HashMap<String, String> getBackup() {
        return backup;
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
