package it.polimi.middleware.akka.messages.storage;

import java.io.Serializable;

import akka.actor.Address;

public class PropagateMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final PutterMessage entry;
    private final Address address;

    public PropagateMessage(PutterMessage entry, Address address) {
        this.entry = entry;
        this.address = address;
    }

    public final PutterMessage getEntry() {
        return entry;
    }
    
    public final Address getAddress() {
    	return address;
    }
}
