package it.polimi.middleware.akka.messages.join;

import java.io.Serializable;

import akka.actor.ActorRef;

public class MasterNotificationMessage implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private ActorRef master;
	
	public MasterNotificationMessage(ActorRef master) {
		this.master = master;
	}

	public final ActorRef getMaster() {
		return master;
	}

	@Override
	public String toString() {
		return "MasterNotificationMessage [master=" + master + "]";
	}

}
