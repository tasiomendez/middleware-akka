package it.polimi.middleware.akka.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import akka.http.javadsl.marshalling.Marshaller;
import akka.http.javadsl.model.MediaTypes;
import akka.http.javadsl.model.RequestEntity;

public class Jackson {

	public static <T> Marshaller<T, RequestEntity> marshaller() {
		final ObjectMapper defaultObjectMapper = new ObjectMapper();
		return marshaller(defaultObjectMapper);
	}

	public static <T> Marshaller<T, RequestEntity> marshaller(ObjectMapper mapper) {
		return Marshaller.wrapEntity(
				u -> toJSON(mapper, u),
				Marshaller.stringToEntity(),
				MediaTypes.APPLICATION_JSON
				);
	}

	private static String toJSON(ObjectMapper mapper, Object object) {
		try {
			return mapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Cannot marshal to JSON: " + object, e);
		}
	}

}