package it.polimi.middleware.akka.messages.heartbeat;

import it.polimi.middleware.akka.node.Reference;

public class NotifyMessage extends Reference {
	
	private static final long serialVersionUID = 1L;

	public NotifyMessage(Reference sender) {
        super(sender);
    }
}
