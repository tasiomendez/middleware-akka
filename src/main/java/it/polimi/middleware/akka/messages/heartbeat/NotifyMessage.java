package it.polimi.middleware.akka.messages.heartbeat;

import it.polimi.middleware.akka.node.Reference;

public class NotifyMessage extends Reference {

    public NotifyMessage(Reference reference) {
        super(reference);
    }
}
