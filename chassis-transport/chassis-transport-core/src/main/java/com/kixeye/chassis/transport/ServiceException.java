package com.kixeye.chassis.transport;

/*
 * #%L
 * Chassis Transport Core
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

import com.kixeye.chassis.transport.dto.ServiceError;

/**
 * A service exception.
 * 
 * @author ebahtijaragic
 */
public class ServiceException extends RuntimeException {
	private static final long serialVersionUID = -8375552421538324398L;

	public final ServiceError error;

	/**
	 * A service exception.
	 *
	 * @param error
	 */
	public ServiceException(ServiceError error) {
		super(error.code + " - " + error.description);

		this.error = error;
	}

	/**
	 * A service exception.
	 *
	 * @param error
	 * @param cause
	 */
	public ServiceException(ServiceError error, Throwable cause) {
		super(error.code + " - " + error.description, cause);

		this.error = error;
	}
}