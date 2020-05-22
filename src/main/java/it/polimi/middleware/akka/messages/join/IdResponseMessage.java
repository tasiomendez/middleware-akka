package it.polimi.middleware.akka.messages.join;

import it.polimi.middleware.akka.node.NodeID;
import scala.Serializable;

public class IdResponseMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final NodeID successor;

    public IdResponseMessage(int id, NodeID successor) {
        this.id = id;
        this.successor = successor;
    }

    public int getId() {
        return id;
    }

    public NodeID getSuccessor() {
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
