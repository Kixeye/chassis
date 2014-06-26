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

import java.util.List;

/**
 * Represents a header.
 * 
 * @author ebahtijaragic
 */
public class Header {
	public String name;
	public List<String> value;
	
	/**
	 * @param name
	 * @param value
	 */
	public Header(String name, List<String> value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Default constructor.
	 */
	public Header() {
	}
}
