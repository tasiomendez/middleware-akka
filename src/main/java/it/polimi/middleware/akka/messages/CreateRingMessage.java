package it.polimi.middleware.akka.messages;

import java.io.Serializable;

public class CreateRingMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;

    public CreateRingMessage(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
