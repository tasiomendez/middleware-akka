package it.polimi.middleware.akka.messages.heartbeat;

import java.io.Serializable;

import it.polimi.middleware.akka.node.NodeType;

public class GetPredecessorResponseMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final NodeType predecessor;

    public GetPredecessorResponseMessage(NodeType predecessor) {
        this.predecessor = predecessor;
    }

    public NodeType getPredecessor() {
        return predecessor;
    }
}
