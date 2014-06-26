package com.kixeye.chassis.transport.http;

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
