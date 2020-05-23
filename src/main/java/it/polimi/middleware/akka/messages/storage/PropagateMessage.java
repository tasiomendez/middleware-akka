package it.polimi.middleware.akka.messages.storage;

import java.io.Serializable;

public class PropagateMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PutterMessage entry;

    public PropagateMessage(PutterMessage entry) {
        this.entry = entry;
    }

    public final PutterMessage getEntry() {
        return entry;
    }
}
