package com.github.knokko.bitser.exceptions;

/**
 * This exception will be thrown when bitser deserializes a reference field, but the target of that reference field
 * seems to be missing. This typically happens when:
 * <ol>
 *     <li>
 *         Struct {@code originalStruct} has a reference to some value {@code vennie}
 *         somewhere in struct {@code withObject}.
 *     </li>
 *     <li>
 *         The original BitStruct is serialized:
 *         {@code byte[] bytes = bitser.serializeToBytesSimple(originalStruct, withObject)}
 *     </li>
 *     <li>{@code vennie} is removed from {@code withObject}</li>
 *     <li>
 *         An attempt is made to deserialize it:
 *         {@code bitser.deserializeFromBytesSimple(originalStruct.getClass(), bytes, withObject)}
 *     </li>
 * </ol>
 */
public class ReferenceBitserException extends BitserException {

	/**
	 * See {@link ReferenceBitserException}
	 */
	public ReferenceBitserException(String message) {
		super(message);
	}
}
