package it.polimi.middleware.akka.messages.heartbeat;

import java.io.Serializable;

import it.polimi.middleware.akka.node.NodeDef;

public class NotifyMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final NodeDef sender;

    public NotifyMessage(NodeDef sender) {
        this.sender = sender;
    }

    public NodeDef getSender() {
        return sender;
    }
}
