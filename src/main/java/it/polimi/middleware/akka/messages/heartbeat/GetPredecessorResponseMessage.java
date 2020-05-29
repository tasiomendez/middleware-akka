package it.polimi.middleware.akka.messages.heartbeat;

import java.io.Serializable;

import it.polimi.middleware.akka.node.Reference;

public class GetPredecessorResponseMessage implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private final Reference predecessor;

	public GetPredecessorResponseMessage(Reference predecessor) {
		this.predecessor = predecessor;
    }

	public final Reference getPredecessor() {
		return predecessor;
	}
}
