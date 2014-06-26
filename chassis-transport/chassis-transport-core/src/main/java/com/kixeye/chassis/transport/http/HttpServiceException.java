package com.kixeye.chassis.transport.http;

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

import com.kixeye.chassis.transport.ServiceException;
import com.kixeye.chassis.transport.dto.ServiceError;

/**
 * An exception that is throw by HTTP services.
 * 
 * @author ebahtijaragic
 */
public class HttpServiceException extends ServiceException {
	private static final long serialVersionUID = -8375552421538324398L;

	public final int httpResponseCode;
	
	/**
	 * An HTTP service exception.
	 * 
	 * @param error
	 */
	public HttpServiceException(ServiceError error, int httpResponseCode) {
		super(error);
		
		this.httpResponseCode = httpResponseCode;
	}
	
	/**
	 * An HTTP service exception.
	 * 
	 * @param error
	 * @param cause
	 */
	public HttpServiceException(ServiceError error, int httpResponseCode, Throwable cause) {
		super(error, cause);

		this.httpResponseCode = httpResponseCode;
	}
}
