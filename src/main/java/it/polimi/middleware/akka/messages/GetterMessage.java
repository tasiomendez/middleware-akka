package it.polimi.middleware.akka.messages;

import java.io.Serializable;

public class GetterMessage implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String key;
	
	public GetterMessage(String key) {
		this.key = key;
	}

	public final String getKey() {
		return key;
	}

	@Override
	public String toString() {
		return "GetterMessage [key=" + key + "]";
	}

}
