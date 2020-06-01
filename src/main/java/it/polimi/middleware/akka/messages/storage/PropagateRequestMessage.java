package it.polimi.middleware.akka.messages.storage;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class PropagateRequestMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> backup;
    private final ActorRef originator;

    private int hopsToLive;

    public PropagateRequestMessage(Map<String, String> backup, ActorRef originator, int hopsToLive) {
        this.backup = backup;
        this.originator = originator;
        this.hopsToLive = hopsToLive;
    }

    public PropagateRequestMessage(PutterMessage backup, ActorRef originator, int hopsToLive) {
        this.backup = new HashMap<>();
        this.backup.put(backup.getKey(), backup.getValue());
        this.originator = originator;
        this.hopsToLive = hopsToLive;
    }

    public final Map<String, String> getBackup() {
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
