package com.comerzzia.unide.api.services.triggers.ui;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

public class SerializableUtil {

	public static <T> T createDataObject(byte[] bytes, Class<T> objectType) {

		ObjectMapper objectMapper = new ObjectMapper();
		// jaxb annotations support
		JaxbAnnotationModule module = new JaxbAnnotationModule();
		objectMapper.registerModule(module);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		try {
			return (T) objectMapper.readValue(new String(bytes), objectType);
		}
		catch (JsonParseException | JsonMappingException e) {
			throw new RuntimeException(e);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static byte[] serializeDataObject(Object object) {
		ObjectMapper objectMapper = new ObjectMapper();
		// jaxb annotations support
		JaxbAnnotationModule module = new JaxbAnnotationModule();
		objectMapper.registerModule(module);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		try {
			return objectMapper.writeValueAsBytes(object);
		}
		catch (JsonParseException | JsonMappingException e) {
			throw new RuntimeException(e);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}