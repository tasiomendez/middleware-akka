package it.polimi.middleware.akka.messages.heartbeat;

import java.io.Serializable;

import it.polimi.middleware.akka.node.NodeDef;

public class GetPredecessorResponseMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final NodeDef predecessor;

    public GetPredecessorResponseMessage(NodeDef predecessor) {
        this.predecessor = predecessor;
    }

    public NodeDef getPredecessor() {
        return predecessor;
    }
}
