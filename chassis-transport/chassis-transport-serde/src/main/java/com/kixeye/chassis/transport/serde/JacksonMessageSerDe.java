package com.kixeye.chassis.transport.serde;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A {link MessageSerDe} that uses Jackson
 *
 * @author dturner@kixeye.com
 */
public interface JacksonMessageSerDe extends MessageSerDe{

    /**
     * Getter for {@link ObjectMapper}
     * @return the objectMapper
     */
    public ObjectMapper getObjectMapper();
}
