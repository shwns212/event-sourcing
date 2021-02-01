package com.jun.event.exception;

public class NotExistEventTypeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public NotExistEventTypeException(String message) {
		super(message);
	}

}
