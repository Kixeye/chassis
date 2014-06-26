package com.kixeye.chassis.transport.http;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.kixeye.chassis.transport.serde.MessageSerDe;

/**
 * A SerDe based message converter.
 * 
 * @author ebahtijaragic
 */
public class SerDeHttpMessageConverter extends AbstractHttpMessageConverter<Object> implements GenericHttpMessageConverter<Object> {
	private final MessageSerDe serDe;
	
	/**
	 * Construct a new {@code ProtostuffJsonHttpMessageConverter}.
	 */
	public SerDeHttpMessageConverter(MessageSerDe serDe) {
		super(serDe.getSupportedMediaTypes());
		this.serDe = serDe;
	}
	
	/**
	 * @see org.springframework.http.converter.GenericHttpMessageConverter#canRead(java.lang.reflect.Type, java.lang.Class, org.springframework.http.MediaType)
	 */
	public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
		return canRead(mediaType);
	}

	/**
	 * @see org.springframework.http.converter.GenericHttpMessageConverter#read(java.lang.reflect.Type, java.lang.Class, org.springframework.http.HttpInputMessage)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
		return serDe.deserialize(inputMessage.getBody(), (Class)type);
	}

	/**
	 * Whether we support this class.
	 * 
	 * @param clazz
	 * @return
	 */
	protected boolean supports(Class<?> clazz) {
		return !(clazz.isPrimitive() || byte[].class.equals(clazz) || ByteBuffer.class.equals(clazz) || String.class.equals(clazz) || InputStream.class.equals(clazz) || Resource.class.equals(clazz));
	}

	/**
	 * @see org.springframework.http.converter.AbstractHttpMessageConverter#readInternal(java.lang.Class, org.springframework.http.HttpInputMessage)
	 */
	protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
		return serDe.deserialize(inputMessage.getBody(), clazz);
	}

	/**
	 * @see org.springframework.http.converter.AbstractHttpMessageConverter#writeInternal(java.lang.Object, org.springframework.http.HttpOutputMessage)
	 */
	protected void writeInternal(Object t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
		serDe.serialize(t, outputMessage.getBody());
	}
}
