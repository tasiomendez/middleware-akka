package it.polimi.middleware.akka.messages.join;

import it.polimi.middleware.akka.node.Reference;

public class FindSuccessorResponseMessage extends Reference {

    private final int request;

    public FindSuccessorResponseMessage(Reference response, int request) {
        super(response);
        this.request = request;
    }

    public int getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return "FindSuccessorResponseMessage [" +
                "successor=" + getActor() +
                ']';
    }
}
