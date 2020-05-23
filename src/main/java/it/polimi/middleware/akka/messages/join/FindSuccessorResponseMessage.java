package it.polimi.middleware.akka.messages.join;

import it.polimi.middleware.akka.node.Reference;

public class FindSuccessorResponseMessage extends Reference {

    public FindSuccessorResponseMessage(Reference successor) {
        super(successor);
    }

    @Override
    public String toString() {
        return "FindSuccessorResponseMessage [" +
                "successor=" + getActor() +
                ']';
    }
}
