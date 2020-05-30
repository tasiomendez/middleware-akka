package it.polimi.middleware.akka.messages.storage;

import java.io.Serializable;
import java.util.HashMap;

import akka.actor.ActorRef;
import it.polimi.middleware.akka.node.Reference;

public class GathererMessage implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private final HashMap<String, String> accumulator = new HashMap<>();
	private final Reference originator;
	private final ActorRef replyTo;
	
	public GathererMessage(HashMap<String, String> acumulator, Reference originator, ActorRef replyTo) {
		this.accumulator.putAll(acumulator);
		this.originator = originator;
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

	@Override
	public String toString() {
		return "GathererRequestMessage [originator=" + originator + "]";
	}
}
