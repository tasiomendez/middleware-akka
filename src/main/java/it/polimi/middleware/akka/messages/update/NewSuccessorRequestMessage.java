package it.polimi.middleware.akka.messages.update;

import it.polimi.middleware.akka.node.Reference;

public class NewSuccessorRequestMessage extends Reference {

	private static final long serialVersionUID = 1L;

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
