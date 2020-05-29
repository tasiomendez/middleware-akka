package it.polimi.middleware.akka.messages.join;

import java.io.Serializable;

public class FindSuccessorRequestMessage implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private final int request;

    public FindSuccessorRequestMessage(int request) {
        this.request = request;
    }

    public int getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return "FindSuccessorRequestMessage [" +
                "node=" + request +
                ']';
    }
}
