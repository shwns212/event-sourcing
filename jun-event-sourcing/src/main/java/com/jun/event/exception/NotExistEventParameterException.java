package com.jun.event.exception;

public class NotExistEventParameterException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NotExistEventParameterException() {
		super("The method empty parameter. event handler method must have only one parameter");
	}
}
