package it.polimi.middleware.akka.messages;

import akka.actor.ActorRef;
import scala.Serializable;

public class FindSuccessorResponseMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ActorRef successor;
    private final int successorId;

    public FindSuccessorResponseMessage(ActorRef successor, int successorId) {
        this.successor = successor;
        this.successorId = successorId;
    }

    public ActorRef getSuccessor() {
        return successor;
    }

    public int getSuccessorId() {
        return successorId;
    }

    @Override
    public String toString() {
        return "FindSuccessorResponseMessage [" +
                "successor=" + successor +
                ']';
    }
}
