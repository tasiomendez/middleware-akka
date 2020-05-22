package it.polimi.middleware.akka.messages.heartbeat;

import java.io.Serializable;

import it.polimi.middleware.akka.node.NodeID;

public class GetPredecessorResponseMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final NodeID predecessor;

    public GetPredecessorResponseMessage(NodeID predecessor) {
        this.predecessor = predecessor;
    }

    public NodeID getPredecessor() {
        return predecessor;
    }
}
