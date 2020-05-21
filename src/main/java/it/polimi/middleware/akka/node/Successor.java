package it.polimi.middleware.akka.node;

import java.io.Serializable;

import akka.actor.ActorRef;

public class Successor extends NodeType implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public Successor(int id, ActorRef actor) {
		super(id, actor);
	}

	@Override
	public String toString() {
		return "Successor [id=" + super.getId() + "]";
	}
	
}
