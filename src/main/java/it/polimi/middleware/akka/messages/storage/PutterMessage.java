package it.polimi.middleware.akka.messages.storage;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class PutterMessage implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String key;
	private String value;
	
	public PutterMessage(String key, String value) {
		this.key = key;
		this.value = value;
	}
	
	public PutterMessage(Map.Entry<String, String> entry) {
		this.key = entry.getKey();
		this.value = entry.getValue();
	}

	public final String getKey() {
		return key;
	}

	public final String getValue() {
		return value;
	}
	
	public final HashMap.Entry<String, String> toHashMapEntry() {
		return new AbstractMap.SimpleEntry<String, String>(this.key, this.value);
	}

	@Override
	public String toString() {
		return "PutterMessage [key=" + key + ", value=" + value + "]";
	}
	
}
