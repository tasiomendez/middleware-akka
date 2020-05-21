package it.polimi.middleware.akka.node;

import java.io.Serializable;

import akka.actor.ActorRef;

public class Predecessor extends NodeType implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public Predecessor(int id, ActorRef actor) {
		super(id, actor);
	}

	@Override
	public String toString() {
		return "Predecessor [id=" + super.getId() + "]";
	}
	
}
