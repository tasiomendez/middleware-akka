package it.polimi.middleware.akka.messages.storage;

import java.io.Serializable;

import akka.actor.ActorRef;

public class GetPartitionRequestMessage implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private final PutterMessage entry;
	private final ActorRef replyTo;
	
	public GetPartitionRequestMessage(PutterMessage entry, ActorRef replyTo) {
		this.entry = entry;
		this.replyTo = replyTo;
	}

	public final PutterMessage getEntry() {
		return entry;
	}
	
	public final ActorRef getReplyTo() {
		return replyTo;
	}

	@Override
	public String toString() {
		return "GetPartitionRequestMessage [key=" + entry.getKey() + "]";
	}
}
