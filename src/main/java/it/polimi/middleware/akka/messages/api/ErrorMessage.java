package it.polimi.middleware.akka.messages.api;

import akka.http.javadsl.model.StatusCode;
import akka.http.javadsl.model.StatusCodes;

public class ErrorMessage extends ReplyMessage {

	public ErrorMessage(StatusCode code) {
		super(code, code.defaultMessage());
	}
	
	public ErrorMessage(Throwable e) {
		super(StatusCodes.INTERNAL_SERVER_ERROR, e.getMessage());
	}
	
}
