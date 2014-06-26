package com.kixeye.chassis.transport;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.kixeye.chassis.transport.dto.ServiceError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.util.List;

/**
 * Maps exceptions into service errors.
 * 
 * @author ebahtijaragic
 */
public class ExceptionServiceErrorMapper {
	public static final String UNKNOWN_ERROR_CODE = "UNKNOWN";
    public static final String VALIDATION_ERROR_CODE = "VALIDATION";
    public static final String SECURITY_ERROR_CODE = "SECURITY";
	
	/**
	 * Maps an exception to an error.
	 * 
	 * @param ex
	 * @return
	 */
	public static ServiceError mapException(Throwable ex) {
		ServiceError error = null;

        if (ex instanceof ServiceException) {
        	ServiceException servEx = (ServiceException)ex;
        	
        	error = servEx.error;
        } else if (ex instanceof MethodArgumentNotValidException) {
        	MethodArgumentNotValidException validationEx = (MethodArgumentNotValidException)ex;
        	
        	List<String> errors = Lists.newArrayList();
        	
        	for (ObjectError objError : validationEx.getBindingResult().getAllErrors()) {
        		errors.add(objError.getObjectName() + ":" + objError.getCode() + ":" + objError.getDefaultMessage());
        	}
        	
        	error = new ServiceError(VALIDATION_ERROR_CODE, Joiner.on("|").join(errors));
        } else {
        	error = new ServiceError(UNKNOWN_ERROR_CODE, ex.getMessage());
        }
        
        return error;
	}
}
