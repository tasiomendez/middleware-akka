package it.polimi.middleware.akka.messages.heartbeat;

import it.polimi.middleware.akka.node.Reference;

public class GetPredecessorResponseMessage extends Reference {
	
	private static final long serialVersionUID = 1L;

	public GetPredecessorResponseMessage(Reference predecessor) {
        super(predecessor);
    }
}
