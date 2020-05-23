package it.polimi.middleware.akka.messages.heartbeat;

import it.polimi.middleware.akka.node.Reference;

public class GetPredecessorResponseMessage extends Reference {

    public GetPredecessorResponseMessage(Reference reference) {
        super(reference);
    }
}
