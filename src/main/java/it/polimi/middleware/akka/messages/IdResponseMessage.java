package it.polimi.middleware.akka.messages;

import akka.actor.ActorRef;
import scala.Serializable;

public class IdResponseMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final ActorRef successor;
    private final int successorId;

    public IdResponseMessage(int id, ActorRef successor, int successorId) {
        this.id = id;
        this.successor = successor;
        this.successorId = successorId;
    }

    public int getId() {
        return id;
    }

    public ActorRef getSuccessor() {
        return successor;
    }

    public int getSuccessorId() {
        return successorId;
    }

    @Override
    public String toString() {
        return "IdResponseMessage [" +
                "id=" + id +
                ", successor=" + successor +
                ']';
    }
}
