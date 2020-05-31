package it.polimi.middleware.akka.messages.storage;

import java.io.Serializable;
import java.util.HashMap;

import akka.actor.Address;

public class PropagateMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final HashMap<String, String> backup;
    private final Address address;

    public PropagateMessage(HashMap<String, String> backup, Address address) {
        this.backup = backup;
        this.address = address;
    }
    
    public PropagateMessage(PropagateRequestMessage msg) {
    	this.backup = msg.getBackup();
    	this.address = msg.getOriginator().path().address();
    }

    public final HashMap<String, String> getBackup() {
        return backup;
    }
    
    public final Address getAddress() {
    	return address;
    }
}
