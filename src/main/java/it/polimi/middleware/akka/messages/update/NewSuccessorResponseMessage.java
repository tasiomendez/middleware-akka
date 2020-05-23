package it.polimi.middleware.akka.messages.update;

import it.polimi.middleware.akka.node.Reference;

public class NewSuccessorResponseMessage extends Reference {

    public NewSuccessorResponseMessage(Reference successor) {
        super(successor);
    }

    @Override
    public String toString() {
        return "NewSuccessorResponseMessage [" +
                "node=" + getActor() +
                ']';
    }

}
