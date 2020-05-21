package it.polimi.middleware.akka.messages;

import it.polimi.middleware.akka.node.NodeDef;
import scala.Serializable;

public class FindSuccessorResponseMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final NodeDef successor;

    public FindSuccessorResponseMessage(NodeDef successor) {
        this.successor = successor;
    }

    public NodeDef getSuccessor() {
        return successor;
    }

    @Override
    public String toString() {
        return "FindSuccessorResponseMessage [" +
                "successor=" + successor +
                ']';
    }
}
