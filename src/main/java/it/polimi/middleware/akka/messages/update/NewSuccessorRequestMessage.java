package it.polimi.middleware.akka.messages.update;

import it.polimi.middleware.akka.node.Reference;

public class NewSuccessorRequestMessage extends Reference {

    public NewSuccessorRequestMessage(Reference sender) {
        super(sender);
    }

    @Override
    public String toString() {
        return "NewSuccessorRequestMessage [" +
                "node=" + getActor() +
                ']';
    }

}
