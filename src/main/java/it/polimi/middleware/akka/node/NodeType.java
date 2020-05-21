package it.polimi.middleware.akka.node;

import java.io.Serializable;

import akka.actor.ActorRef;

public class NodeType implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private int id;
	private ActorRef actor;
	
	public NodeType(int id, ActorRef actor) {
		this.id = id;
		this.actor = actor;
	}

	public final int getId() {
		return id;
	}

	public final void setId(int id) {
		this.id = id;
	}

	public final ActorRef getActor() {
		return actor;
	}

	public final void setActor(ActorRef actor) {
		this.actor = actor;
	}
	
	public boolean isNull() {
		return this.actor == null;
	}

	@Override
	public String toString() {
		return "Node [id=" + id + "]";
	}

	@Override
	public int hashCode() {
		return this.actor.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		NodeType other = (NodeType) obj;
		return other.id == this.id && this.actor.equals(other.actor);
	}
	
}
