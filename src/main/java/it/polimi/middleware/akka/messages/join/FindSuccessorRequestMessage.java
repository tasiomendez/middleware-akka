package it.polimi.middleware.akka.messages.join;

import it.polimi.middleware.akka.node.Reference;

public class FindSuccessorRequestMessage extends Reference {

    public FindSuccessorRequestMessage(Reference sender) {
        super(sender);
    }

    @Override
    public String toString() {
        return "FindSuccessorRequestMessage [" +
                "node=" + getActor() +
                ']';
    }
}
