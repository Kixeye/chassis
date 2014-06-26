package com.kixeye.chassis.transport;

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
