package it.polimi.middleware.akka.messages.heartbeat;

import java.io.Serializable;

import it.polimi.middleware.akka.node.NodeType;

public class NotifyMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final NodeType sender;

    public NotifyMessage(NodeType sender) {
        this.sender = sender;
    }

    public NodeType getSender() {
        return sender;
    }
}
