package it.polimi.middleware.akka.messages.join;

import akka.actor.ActorRef;

import java.io.Serializable;

public class MasterNotificationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ActorRef master;

    public MasterNotificationMessage(ActorRef master) {
        this.master = master;
    }

    public final ActorRef getMaster() {
        return master;
    }

    @Override
    public String toString() {
        return "MasterNotificationMessage [master=" + master + "]";
    }

}
