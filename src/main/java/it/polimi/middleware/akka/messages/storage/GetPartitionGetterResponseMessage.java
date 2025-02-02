package it.polimi.middleware.akka.messages.storage;

import java.io.Serializable;

import akka.actor.ActorRef;

public class GetPartitionGetterResponseMessage implements Serializable {

	private static final long serialVersionUID = 1L;

	private final GetterMessage entry;
	private final ActorRef replyTo;
	
	public GetPartitionGetterResponseMessage(GetterMessage entry, ActorRef replyTo) {
		this.entry = entry;
		this.replyTo = replyTo;
	}

	public final GetterMessage getEntry() {
		return entry;
	}

	public final ActorRef getReplyTo() {
		return replyTo;
	}

	@Override
	public String toString() {
		return "GetPartitionGetterResponseMessage [entry=" + entry + ", replyTo=" + replyTo + "]";
	}

}
