package it.polimi.middleware.akka.messages;

import it.polimi.middleware.akka.node.NodeType;
import scala.Serializable;

public class FindSuccessorResponseMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final NodeType successor;

    public FindSuccessorResponseMessage(NodeType successor) {
        this.successor = successor;
    }

    public NodeType getSuccessor() {
        return successor;
    }

    @Override
    public String toString() {
        return "FindSuccessorResponseMessage [" +
                "successor=" + successor +
                ']';
    }
}
