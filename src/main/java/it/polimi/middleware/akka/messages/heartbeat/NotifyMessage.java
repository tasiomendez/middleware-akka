package it.polimi.middleware.akka.messages.heartbeat;

import java.io.Serializable;

public class NotifyMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;

    public NotifyMessage(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
