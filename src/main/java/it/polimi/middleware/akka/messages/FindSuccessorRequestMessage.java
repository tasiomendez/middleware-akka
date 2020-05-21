package it.polimi.middleware.akka.messages;

import java.io.Serializable;

import it.polimi.middleware.akka.node.NodeType;

public class FindSuccessorRequestMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final NodeType sender;

    public FindSuccessorRequestMessage(NodeType sender) {
        this.sender = sender;
    }

    public NodeType getSender() {
        return sender;
    }

    @Override
    public String toString() {
        return "FindSuccessorRequestMessage [" +
                "node=" + sender +
                ']';
    }
}
