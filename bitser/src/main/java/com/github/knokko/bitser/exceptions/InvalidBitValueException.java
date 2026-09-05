package com.github.knokko.bitser.exceptions;

/**
 * This exception is thrown by bitser when it tries to serialize a value that is invalid for a field. An example would
 * be a field annotated with {@code IntegerField(minValue = 10, ...)}, whose value is 5.
 */
public class InvalidBitValueException extends BitserException {

	/**
	 * This exception is thrown by bitser when it tries to serialize a value that is invalid for a field.
	 * An example would be a field annotated with {@code IntegerField(minValue = 10, ...)}, whose value is 5.
	 */
	public InvalidBitValueException(String message) {
		super(message);
	}
}
