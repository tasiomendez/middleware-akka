package it.polimi.middleware.akka.messages.api;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import akka.actor.Address;
import akka.http.javadsl.model.StatusCodes;

public class SuccessMessage extends ReplyMessage {
	
	private static final long serialVersionUID = 1L;
	
	@JsonInclude(Include.NON_EMPTY)
	private HashMap<String, String> storage;
	
	@JsonInclude(Include.NON_EMPTY)
	private HashMap<Address, HashMap<String, String>> backup;
	
	@JsonInclude(Include.NON_NULL)
	private String node;
	
	public SuccessMessage(String key, String value, String node) {
		super(StatusCodes.ACCEPTED, "OK");
		this.storage = new HashMap<String, String>();
		this.storage.put(key, value);
		this.node = node;
	}
	
	public SuccessMessage(HashMap<String, String> storage) {
		super(StatusCodes.ACCEPTED, "OK");
		this.storage = storage;
	}
	
	public SuccessMessage(HashMap<String, String> storage, HashMap<Address, HashMap<String, String>> backup) {
		super(StatusCodes.ACCEPTED, "OK");
		this.storage = storage;
		this.backup = backup;
	}

	public final HashMap<String, String> getStorage() {
		return storage;
	}

	public final HashMap<Address, HashMap<String, String>> getBackup() {
		return backup;
	}

	public final String getNode() {
		return node;
	}

}
