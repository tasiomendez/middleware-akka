package it.polimi.middleware.akka.messages.storage;

import java.io.Serializable;

public class GetterMessage implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String key;
	private boolean all = false;
	
	public GetterMessage() {
		this.all = true;
	}
	
	public GetterMessage(String key) {
		this.key = key;
		this.all = false;
	}

	public final String getKey() {
		return key;
	}

	public final boolean isAll() {
		return all;
	}

	@Override
	public String toString() {
		return "GetterMessage [key=" + key + "]";
	}

}
