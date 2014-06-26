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

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.kixeye.chassis.transport.dto.ServiceError;

/**
 * Holds the messages that are registered in the system.
 * 
 * @author ebahtijaragic
 */
@Component
public class WebSocketMessageRegistry {
	private ConcurrentHashMap<String, String> typeIdToClass = new ConcurrentHashMap<String, String>();
	private ConcurrentHashMap<String, String> classToTypeId = new ConcurrentHashMap<String, String>();
	
	@PostConstruct
	public void initialize() {
		registerType("error", ServiceError.class);
	}
	
	/**
	 * Gets all the type IDs.
	 * 
	 * @return
	 */
	public Set<String> getTypeIds() {
		return Collections.unmodifiableSet(typeIdToClass.keySet());
	}
	
	/**
	 * Registers a type.
	 * 
	 * @param name
	 * @param type
	 */
	public synchronized void registerType(String typeId, Class<?> clazz) {
		typeIdToClass.put(typeId, clazz.getName());
		classToTypeId.put(clazz.getName(), typeId);
	}
	
	/**
	 * Gets a class by the typeid.
	 * 
	 * @param typeId
	 * @return
	 */
	public Class<?> getClassByTypeId(String typeId) {
		try {
			String className = typeIdToClass.get(typeId);
			
			if (className != null) {
				return Class.forName(typeIdToClass.get(typeId));
			} else {
				return null;
			}
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
	
	/**
	 * Returns true if class is registered.
	 * 
	 * @param clazz
	 * @return
	 */
	public boolean isClassRegistered(Class<?> clazz) {
		return classToTypeId.contains(clazz.getName());
	}

	/**
	 * Gets a typeid by the class.
	 * 
	 * @param typeId
	 * @return
	 */
	public String getTypeIdByClass(Class<?> clazz) {
		return classToTypeId.get(clazz.getName());
	}
}
