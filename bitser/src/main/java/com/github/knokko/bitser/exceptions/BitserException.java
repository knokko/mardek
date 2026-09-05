package com.github.knokko.bitser.exceptions;

/**
 * The superclass of all exceptions that will be thrown by bitser.
 */
public abstract class BitserException extends RuntimeException {

	BitserException(String message) {
		super(message);
	}

	BitserException(String message, Throwable cause) {
		super(message, cause);
	}
}
