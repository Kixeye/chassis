package com.kixeye.chassis.transport.websocket;

/*
 * #%L
 * Java Transport API
 * %%
 * Copyright (C) 2014 KIXEYE, Inc
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.io.IOException;
import java.nio.ByteBuffer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.kixeye.chassis.transport.dto.Envelope;
import com.kixeye.chassis.transport.dto.Header;
import com.kixeye.chassis.transport.serde.MessageSerDe;

/**
 * A immutable wrapper around the {@link Envelope}.
 * 
 * @author ebahtijaragic
 */
public class WebSocketEnvelope {
	private final Envelope base;
	
	private Multimap<String, String> headerCache = null;
	
	/**
	 * @param base
	 */
	public WebSocketEnvelope(Envelope base) {
		this.base = base;
	}

	/**
	 * @return the action
	 */
	public String getAction() {
		return base.action;
	}

	/**
	 * @return the transactionId
	 */
	public String getTransactionId() {
		return base.transactionId;
	}

	/**
	 * @return the typeId
	 */
	public String getTypeId() {
		return base.typeId;
	}

	/**
	 * @return the payload
	 */
	public ByteBuffer getPayload() {
		return base.payload != null ? base.payload.asReadOnlyBuffer() : null;
	}
	
	/**
	 * Returns true if this envelope has a payload.
	 * 
	 * @return
	 */
	public boolean hasPayload() {
		return base.payload != null;
	}

	/**
	 * Gets the headers.
	 * 
	 * @return
	 */
    public Multimap<String,String> getHeaders(){
    	if (headerCache == null) {
    		generateHeaderCache();
    	}
    	
    	return headerCache;
    }
    
    /**
     * Serializes the source envelope.
     * 
     * @param serDe
     * @return
     * @throws IOException 
     */
    public byte[] serializeEnvelope(MessageSerDe serDe) throws IOException {
    	return serDe.serialize(base);
    }
    
    /**
     * Generates the header cache.
     */
    private synchronized void generateHeaderCache() {
    	if (headerCache != null) {
    		// already generated cache
    		return;
    	}
    	
    	// generate the multimap
    	Multimap<String, String> headers = HashMultimap.create();
    	
    	if (base.headers != null) {
    		for (Header header : base.headers) {
    			headers.putAll(header.name, header.value);
    		}
    	}
    	
    	headerCache = Multimaps.unmodifiableMultimap(headers);
    }
}
