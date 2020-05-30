package it.polimi.middleware.akka.messages.join;

import java.io.Serializable;
import java.util.Map;

public class MoveStorageRequestMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> move;

    public MoveStorageRequestMessage(Map<String, String> move) {
        this.move = move;
    }

    public Map<String, String> getMove() {
        return move;
    }
}
