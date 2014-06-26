package com.kixeye.chassis.transport.http;

import javax.validation.Validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Validates HTTP messages.
 * 
 * @author ebahtijaragic
 */
@ControllerAdvice
public class HttpValidator {
	@Autowired
	private Validator messageValidator;
	
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setValidator(new SpringValidatorAdapter(messageValidator));
	}
}
