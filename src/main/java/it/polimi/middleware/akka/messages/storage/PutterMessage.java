package it.polimi.middleware.akka.messages.storage;

import java.io.Serializable;

public class PutterMessage implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String key;
	private String value;
	
	public PutterMessage(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public final String getKey() {
		return key;
	}

	public final String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "PutterMessage [key=" + key + ", value=" + value + "]";
	}
	
}
