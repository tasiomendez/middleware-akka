package it.polimi.middleware.akka.messages;

import java.io.Serializable;

import it.polimi.middleware.akka.node.NodeDef;

public class FindSuccessorRequestMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final NodeDef sender;

    public FindSuccessorRequestMessage(NodeDef sender) {
        this.sender = sender;
    }

    public NodeDef getSender() {
        return sender;
    }

    @Override
    public String toString() {
        return "FindSuccessorRequestMessage [" +
                "node=" + sender +
                ']';
    }
}
