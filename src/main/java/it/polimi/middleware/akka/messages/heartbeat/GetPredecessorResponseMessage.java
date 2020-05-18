package it.polimi.middleware.akka.messages.heartbeat;

import akka.actor.ActorRef;

import java.io.Serializable;

public class GetPredecessorResponseMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int predecessorId;
    private final ActorRef predecessor;

    public GetPredecessorResponseMessage(int predecessorId, ActorRef predecessor) {
        this.predecessorId = predecessorId;
        this.predecessor = predecessor;
    }

    public int getPredecessorId() {
        return predecessorId;
    }

    public ActorRef getPredecessor() {
        return predecessor;
    }
}
