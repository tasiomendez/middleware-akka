package it.polimi.middleware.akka.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import akka.http.javadsl.marshalling.Marshaller;
import akka.http.javadsl.model.MediaTypes;
import akka.http.javadsl.model.RequestEntity;

public class Jackson {

	/**
	 * Returns a marshaller using a default mapper in order to convert to a 
	 * JSON object.
	 * 
	 * @return marshaller
	 */
	public static <T> Marshaller<T, RequestEntity> marshaller() {
		final ObjectMapper defaultObjectMapper = new ObjectMapper()
				.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
		return marshaller(defaultObjectMapper);
	}

	/**
	 * Returns a marshaller using the mapper provided in order to convert to a 
	 * JSON object.
	 * 
	 * @param mapper
	 * @return marshaller
	 */
	public static <T> Marshaller<T, RequestEntity> marshaller(ObjectMapper mapper) {
		return Marshaller.wrapEntity(
				u -> toJSON(mapper, u),
				Marshaller.stringToEntity(),
				MediaTypes.APPLICATION_JSON
				);
	}

	/**
	 * Convert a Java Object into a JSON object using the mapper provided.  
	 * 
	 * @param mapper 
	 * @param object to convert
	 * @return Object in JSON format stringified
	 */
	private static String toJSON(ObjectMapper mapper, Object object) {
		try {
			return mapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Cannot marshal to JSON: " + object, e);
		}
	}

}
