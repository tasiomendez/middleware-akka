package it.polimi.middleware.akka.messages;

import it.polimi.middleware.akka.node.NodeDef;
import scala.Serializable;

public class IdResponseMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final NodeDef successor;

    public IdResponseMessage(int id, NodeDef successor) {
        this.id = id;
        this.successor = successor;
    }

    public int getId() {
        return id;
    }

    public NodeDef getSuccessor() {
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
