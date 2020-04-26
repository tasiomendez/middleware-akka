package it.polimi.middleware.akka.messages;

import java.util.HashMap;

public class ReplyMessage {
	
	private HashMap<String, String> storage;
	private String node;

	public ReplyMessage(String key, String value, String node) {
		this.storage = new HashMap<String, String>();
		this.storage.put(key, value);
		this.node = node;
	}
	
	public ReplyMessage(HashMap<String, String> storage) {
		this.storage = storage;
	}
	
	public final HashMap<String, String> getStorage() {
		return storage;
	}

	public final String getNode() {
		return node;
	}
	
	public final int size() {
		return storage.size();
	}

	@Override
	public String toString() {
		return "ReplyMessage [storage=" + storage + ", node=" + node + "]";
	}
	
}
