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
 * Thrown when an action could not be performed due for security purposes. Error code and description
 * is intentionally vague.
 *
 * @author dturner@kixeye.com
 */
public class SecurityException extends ServiceException{
	private static final long serialVersionUID = -8030168029539007907L;

	private static final ServiceError error = new ServiceError(ExceptionServiceErrorMapper.SECURITY_ERROR_CODE,"Security Error");

    public SecurityException() {
        super(error);
    }
}
