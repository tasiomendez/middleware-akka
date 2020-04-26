package it.polimi.middleware.akka.messages.api;

import akka.http.javadsl.model.StatusCode;

public class ReplyMessage {
	
	private StatusCode code;
	private String message;
	
	public ReplyMessage(StatusCode code, String message) {
		this.code = code;
		this.message = message;
	}
	
	public ReplyMessage(StatusCode code) {
		this.code = code;
	}

	public final int getCode() {
		return code.intValue();
	}

	public final String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return "RepplyMessage [code=" + code + ", message=" + message + "]";
	}

}
