package it.polimi.middleware.akka.messages;

import it.polimi.middleware.akka.node.Successor;
import scala.Serializable;

public class IdResponseMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final Successor successor;

    public IdResponseMessage(int id, Successor successor) {
        this.id = id;
        this.successor = successor;
    }

    public int getId() {
        return id;
    }

    public Successor getSuccessor() {
        return successor;
    }
    
    @Override
    public String toString() {
        return "IdResponseMessage [" +
                "id=" + id +
                ", successor=" + successor +
                ']';
    }
}
