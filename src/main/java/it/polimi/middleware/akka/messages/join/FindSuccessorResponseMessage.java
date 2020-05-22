package it.polimi.middleware.akka.messages.join;

import it.polimi.middleware.akka.node.NodeID;
import scala.Serializable;

public class FindSuccessorResponseMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final NodeID successor;

    public FindSuccessorResponseMessage(NodeID successor) {
        this.successor = successor;
    }

    public NodeID getSuccessor() {
        return successor;
    }

    @Override
    public String toString() {
        return "FindSuccessorResponseMessage [" +
                "successor=" + successor +
                ']';
    }
}
