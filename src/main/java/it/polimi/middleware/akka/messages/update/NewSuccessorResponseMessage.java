package it.polimi.middleware.akka.messages.update;

import java.io.Serializable;

import it.polimi.middleware.akka.node.NodeID;

public class NewSuccessorResponseMessage implements Serializable {
	
	private static final long serialVersionUID = 1L;

    private final NodeID successor;

    public NewSuccessorResponseMessage(NodeID successor) {
        this.successor = successor;
    }

    public NodeID getSuccessor() {
        return successor;
    }

    @Override
    public String toString() {
        return "NewSuccessorResponseMessage [" +
                "node=" + successor +
                ']';
    }

}
