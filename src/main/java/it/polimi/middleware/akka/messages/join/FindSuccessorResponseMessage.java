package it.polimi.middleware.akka.messages.join;

import java.io.Serializable;

import it.polimi.middleware.akka.node.Reference;

public class FindSuccessorResponseMessage implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private final Reference response;
	private final int request;

    public FindSuccessorResponseMessage(Reference response, int request) {
    	this.response = response;
        this.request = request;
    }

    public final Reference getResponse() {
		return response;
	}

	public int getIdRequest() {
        return request;
    }

    @Override
    public String toString() {
        return "FindSuccessorResponseMessage [" +
                "successor=" + this.response.getActor() +
                ']';
    }
}
