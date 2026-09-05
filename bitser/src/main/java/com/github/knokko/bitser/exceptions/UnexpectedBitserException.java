package com.github.knokko.bitser.exceptions;

/**
 * Indicates an exception that should <b>not</b> be thrown. If bitser throws it anyway, it is a bug in bitser.
 */
public class UnexpectedBitserException extends BitserException {

	/**
	 * Indicates an exception that should <b>not</b> be thrown. If bitser throws it anyway, it is a bug in bitser.
	 */
	public UnexpectedBitserException(String message) {
		super(message);
	}
}
