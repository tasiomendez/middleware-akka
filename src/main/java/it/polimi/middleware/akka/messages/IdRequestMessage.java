package it.polimi.middleware.akka.messages;

import java.io.Serializable;

import akka.cluster.Member;

public class IdRequestMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    
    // New member joined to cluster
    private final Member member;

	public IdRequestMessage(Member member) {
    	this.member = member;
    }
    
    public final Member getMember() {
		return member;
	}
    
}
