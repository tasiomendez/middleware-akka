package it.polimi.middleware.akka.messages;

import akka.actor.ActorRef;

import java.io.Serializable;

public class PropagatePutterMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * True if the message has been propagated from a node to another node, false if it has been sent from {@link
     * it.polimi.middleware.akka.master.partitionmanager.PartitionManager}.
     */
    private final boolean isPropagated;
    /** Number of times the message has to be forwarded, decreases every time. */
    private final int hopsToLive;
    private final PutterMessage msg;
    /** The reference to the first actor the message has been sent to. */
    private final ActorRef initiator;

    private PropagatePutterMessage(boolean isPropagated, int hopsToLive, PutterMessage msg, ActorRef initiator) {
        this.isPropagated = isPropagated;
        this.hopsToLive = hopsToLive;
        this.msg = msg;
        this.initiator = initiator;
    }

    public PropagatePutterMessage(int hopsToLive, PutterMessage msg, ActorRef initiator) {
        this.isPropagated = false;
        this.hopsToLive = hopsToLive;
        this.msg = msg;
        this.initiator = initiator;
    }

    /**
     * Returns a new instance of {@link PropagatePutterMessage}, where the {@link PropagatePutterMessage#hopsToLive}
     * counter has been decreased by one and the flag {@link PropagatePutterMessage#isPropagated} is set to false.
     *
     * @return a new instance of {@link PropagatePutterMessage}
     */
    public PropagatePutterMessage propagate() {
        return new PropagatePutterMessage(true, hopsToLive - 1, msg, initiator);
    }

    public boolean isPropagated() {
        return isPropagated;
    }

    public int getHopsToLive() {
        return hopsToLive;
    }

    public PutterMessage getMsg() {
        return msg;
    }

    public ActorRef getInitiator() {
        return initiator;
    }
}
