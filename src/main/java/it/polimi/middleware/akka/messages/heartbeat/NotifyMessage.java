package it.polimi.middleware.akka.messages.heartbeat;

import java.io.Serializable;

import it.polimi.middleware.akka.node.Reference;

public class NotifyMessage implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private final Reference sender;

	public NotifyMessage(Reference sender) {
		this.sender = sender;
    }

	public final Reference getSender() {
		return sender;
	}
}
