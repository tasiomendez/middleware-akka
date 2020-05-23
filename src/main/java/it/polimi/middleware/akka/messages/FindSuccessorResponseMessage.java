package it.polimi.middleware.akka.messages;

import it.polimi.middleware.akka.node.Reference;

public class FindSuccessorResponseMessage extends Reference {

    public FindSuccessorResponseMessage(Reference reference) {
        super(reference);
    }
}
