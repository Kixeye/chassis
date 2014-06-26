package com.kixeye.chassis.transport;

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