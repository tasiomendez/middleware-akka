package it.polimi.middleware.akka.node;

import akka.actor.ActorRef;

import java.io.Serializable;

/**
 * Represents the association of an id of a node and the actual {@link ActorRef} of the actor running on that node. It
 * is meant to be mutable and swapped around between nodes.
 */
public class Reference implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private ActorRef actorRef;

    public Reference(int id, ActorRef actorRef) {
        this.id = id;
        this.actorRef = actorRef;
    }

    public Reference(Reference other) {
        this.id = other.getId();
        this.actorRef = other.getActorRef();
    }

    private Reference(Integer id, ActorRef actorRef) {
        this.id = id;
        this.actorRef = actorRef;
    }

    public static Reference empty() {
        return new Reference(null, null);
    }

    public final void update(int id, ActorRef actorRef) {
        this.id = id;
        this.actorRef = actorRef;
    }

    public final void update(Reference other) {
        this.id = other.getId();
        this.actorRef = other.getActorRef();
    }

    public final Integer getId() {
        return id == null ? -1 : id;
    }

    public final ActorRef getActorRef() {
        return actorRef;
    }

    @Override
    public String toString() {
        return id == null ? "null" : id.toString();
    }
}
