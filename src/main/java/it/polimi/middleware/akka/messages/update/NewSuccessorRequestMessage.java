package it.polimi.middleware.akka.messages.update;

import java.io.Serializable;

import it.polimi.middleware.akka.node.NodeID;

public class NewSuccessorRequestMessage implements Serializable {
	
	private static final long serialVersionUID = 1L;

    private final NodeID sender;

    public NewSuccessorRequestMessage(NodeID sender) {
        this.sender = sender;
    }

    public NodeID getSender() {
        return sender;
    }

    @Override
    public String toString() {
        return "NewSuccessorRequestMessage [" +
                "node=" + sender +
                ']';
    }
   
}
