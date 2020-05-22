package it.polimi.middleware.akka.messages.heartbeat;

import java.io.Serializable;

import it.polimi.middleware.akka.node.NodeID;

public class NotifyMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final NodeID sender;

    public NotifyMessage(NodeID sender) {
        this.sender = sender;
    }

    public NodeID getSender() {
        return sender;
    }
}
