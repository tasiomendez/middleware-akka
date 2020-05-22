package it.polimi.middleware.akka.messages.join;

import java.io.Serializable;

import it.polimi.middleware.akka.node.NodeID;

public class FindSuccessorRequestMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final NodeID sender;

    public FindSuccessorRequestMessage(NodeID sender) {
        this.sender = sender;
    }

    public NodeID getSender() {
        return sender;
    }

    @Override
    public String toString() {
        return "FindSuccessorRequestMessage [" +
                "node=" + sender +
                ']';
    }
}
