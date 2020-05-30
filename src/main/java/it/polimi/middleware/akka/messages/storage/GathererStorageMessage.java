package it.polimi.middleware.akka.messages.storage;

import java.io.Serializable;
import java.util.HashMap;

import akka.actor.ActorRef;
import it.polimi.middleware.akka.node.Reference;

public class GathererStorageMessage implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private final HashMap<String, String> accumulator = new HashMap<>();
	private final Reference originator;
	private final ActorRef replyTo;
	
	public GathererStorageMessage(GathererMessage message) {
		this.accumulator.putAll(message.getAccumulator());
		this.originator = message.getOriginator();
		this.replyTo = message.getReplyTo();
	}
	
	public GathererStorageMessage(HashMap<String, String> accumulator, ActorRef replyTo) {
		this.accumulator.putAll(accumulator);
		this.originator = null;
		this.replyTo = replyTo;
	}

	public final HashMap<String, String> getAccumulator() {
		return accumulator;
	}
	
	public final Reference getOriginator() {
		return originator;
	}

	public final ActorRef getReplyTo() {
		return replyTo;
	}

	public GathererStorageMessage sum(HashMap<String, String> accumulator) {
		this.accumulator.putAll(accumulator);
		return this;
	}

}
