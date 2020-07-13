package it.polimi.middleware.akka.node;

import akka.actor.ActorRef;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents the association of an id of a node and the actual {@link ActorRef} of the actor running on that node. It
 * is meant to be mutable and swapped around between nodes.
 */
public class Reference implements Serializable {

    public static final int ID_NULL = -1;

    private static final long serialVersionUID = 1L;

    private Integer id;
    private ActorRef actor;

    public Reference(int id, ActorRef actor) {
        this.id = id;
        this.actor = actor;
    }

    public Reference(Reference other) {
        this.id = other.getId();
        this.actor = other.getActor();
    }

    private Reference(Integer id, ActorRef actor) {
        this.id = id;
        this.actor = actor;
    }

    public static Reference empty() {
        return new Reference(null, null);
    }

    public final boolean isNull() {
        return this.actor == null;
    }

    public final void update(int id, ActorRef actorRef) {
        this.id = id;
        this.actor = actorRef;
    }

    public final void update(Reference other) {
        this.id = other.getId();
        this.actor = other.getActor();
    }

    public final Integer getId() {
        return id == null ? ID_NULL : id;
    }

    public final ActorRef getActor() {
        return actor;
    }

    public final Reference copy() {
        return new Reference(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, actor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reference reference = (Reference) o;
        return id.equals(reference.id) &&
                actor.equals(reference.actor);
    }

    @Override
    public String toString() {
        return "Node[" + (id == null ? "null" : id) + "]";
    }
}
