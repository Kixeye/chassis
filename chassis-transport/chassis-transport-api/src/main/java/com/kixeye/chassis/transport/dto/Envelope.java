package com.kixeye.chassis.transport.dto;

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


import java.nio.ByteBuffer;
import java.util.List;

/**
 * Represents an envelope.
 * 
 * @author ebahtijaragic
 */
public class Envelope {
	public String action;
	public String transactionId;
	public String typeId;
	public ByteBuffer payload;
    public List<Header> headers;

    public Envelope(String action, String typeId, String transactionId, ByteBuffer payload) {
        this.action = action;
        this.typeId = typeId;
        this.transactionId = transactionId;
        this.payload = payload;
    }

    public Envelope(String action, String typeId, String transactionId, List<Header> headers, ByteBuffer payload) {
        this.action = action;
        this.typeId = typeId;
        this.transactionId = transactionId;
        this.payload = payload;
        this.headers = headers;
    }
	
	public Envelope() {
	}
}
