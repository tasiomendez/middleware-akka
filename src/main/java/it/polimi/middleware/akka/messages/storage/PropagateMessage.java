package it.polimi.middleware.akka.messages.storage;

import akka.actor.Address;

import java.io.Serializable;
import java.util.Map;

public class PropagateMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<String, String> backup;
    private final Address address;

    public PropagateMessage(Map<String, String> backup, Address address) {
        this.backup = backup;
        this.address = address;
    }
    
    public PropagateMessage(PropagateRequestMessage msg) {
    	this.backup = msg.getBackup();
    	this.address = msg.getOriginator().path().address();
    }

    public final Map<String, String> getBackup() {
        return backup;
    }
    
    public final Address getAddress() {
    	return address;
    }
}
