package it.polimi.middleware.akka.messages;

import it.polimi.middleware.akka.node.Reference;
import scala.Serializable;

public class IdResponseMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final Reference successor;

    public IdResponseMessage(int id, Reference successor) {
        this.id = id;
        this.successor = successor;
    }

    public int getId() {
        return id;
    }

    public Reference getSuccessor() {
        return successor;
    }
}
