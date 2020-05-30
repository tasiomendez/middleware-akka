package it.polimi.middleware.akka.messages.storage;

import java.io.Serializable;

import akka.actor.Address;

public class RestoreRequestMessage implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private final Address address;
	
	public RestoreRequestMessage(Address address) {
		this.address = address;
	}

	public final Address getAddress() {
		return address;
	}

	@Override
	public String toString() {
		return "RestoreRequestMessage [address=" + address + "]";
	}

}
