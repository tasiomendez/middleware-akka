package it.polimi.middleware.akka.messages;

import java.io.Serializable;

public class FindSuccessorRequestMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;

    public FindSuccessorRequestMessage(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "FindSuccessorRequestMessage [" +
                "id=" + id +
                ']';
    }
}
