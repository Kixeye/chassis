package com.kixeye.chassis.transport.websocket.docs;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Map;

import com.dyuproject.protostuff.WireFormat;
import com.dyuproject.protostuff.runtime.MappedSchema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import com.kixeye.chassis.transport.websocket.WebSocketMessageRegistry;

/**
 * Generates Protobuf schemas.
 * 
 * @author ebahtijaragic
 */
public final class ProtobufSchemaGenerator {
	private ProtobufSchemaGenerator() {}

	@SuppressWarnings({ "rawtypes" })
	public static String generateSchema(Class<?> clazz, Map<String, String> messages, WebSocketMessageRegistry messageRegistry) throws Exception {
		String messageName = clazz.getSimpleName();
		
		if (messageRegistry != null) {
			String typeId = messageRegistry.getTypeIdByClass(clazz);
			
			if (typeId != null) {
				messageName = typeId;
			}
		}
		
		if (messages.containsKey(messageName)) {
			return messageName;
		}
		
		StringWriter coreWriter = new StringWriter();
		PrintWriter messageWriter = new PrintWriter(coreWriter);
		
		messages.put(messageName, "");
		
		messageWriter.append("message ").append(messageName).append(" {").println();
		
		Class<?> runtimeSchemaClass = MappedSchema.class;
		
		Field fieldsField = runtimeSchemaClass.getDeclaredField("fields");
		fieldsField.setAccessible(true);

		MappedSchema<?> schema = RuntimeSchema.createFrom(clazz);
		
		MappedSchema.Field[] fields = (MappedSchema.Field[])fieldsField.get(schema);
		
		for (int i = 0; i < fields.length; i++) {
			MappedSchema.Field field = fields[i];
			
			if (field != null) {
				messageWriter.append("\t").append(field.repeated ? "repeated" : "optional").append(" ");
				
				if (field.type == WireFormat.FieldType.MESSAGE) {
					// lets try the standard route
					Field typeClassField = field.getClass().getField("typeClass");
					typeClassField.setAccessible(true);
					Class<?> typeClass = (Class<?>)typeClassField.get(field);
					
					if (typeClass.isAssignableFrom(ByteBuffer.class)) {
						messageWriter.append("bytes");
					} else {
						messageWriter.append(generateSchema(typeClass, messages, messageRegistry));
					}
				} else {
					messageWriter.append(field.type.name().toLowerCase());
				}
				
				messageWriter.append(" ").append(field.name).append(" = " + field.number).append(";").println();
			}
		}

		messageWriter.append("}").println();

		messages.put(messageName, coreWriter.toString());
		
		return messageName;
	}
}
