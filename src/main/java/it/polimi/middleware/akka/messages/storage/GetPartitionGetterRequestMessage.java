package it.polimi.middleware.akka.messages.storage;

import akka.actor.ActorRef;

import java.io.Serializable;

public class GetPartitionGetterRequestMessage implements Serializable {

	private static final long serialVersionUID = 1L;

	private final GetterMessage entry;
	private final ActorRef replyTo;

	private int hopsToLive;

	public GetPartitionGetterRequestMessage(int hopsToLive , GetterMessage entry, ActorRef replyTo) {
		this.hopsToLive = hopsToLive;
		this.entry = entry;
		this.replyTo = replyTo;
	}

	public final GetPartitionGetterRequestMessage forward() {
		this.hopsToLive -= 1;
		return this;
	}

	public final boolean alive() {
		return hopsToLive > 0;
	}

	public final GetterMessage getEntry() {
		return entry;
	}

	public final ActorRef getReplyTo() {
		return replyTo;
	}

	@Override
	public String toString() {
		return "GetPartitionGetterMessage [entry=" + entry + ", replyTo=" + replyTo + "]";
	}

}
