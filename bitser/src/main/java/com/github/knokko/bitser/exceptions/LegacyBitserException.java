package com.github.knokko.bitser.exceptions;

/**
 * This exception will be thrown when bitser is deserializing a backward-compatible BitStruct, but encounters a
 * deserialized value that no longer fits. This happens for instance when you:
 * <ol>
 *     <li>
 *         Create a BitStruct with the following field:
 *         {@code @BitField(id=1) @IntegerField(expectUniform=true) public int exampleInt}
 *     </li>
 *     <li>
 *         Serialize an instance of that struct to a file.
 *     </li>
 *     <li>
 *         Change the field to:
 *         {@code @BitField(id=1) public HashMap<String, String> exampleMap}
 *     </li>
 *     <li>
 *         Deserialize the new struct from that file.
 *     </li>
 * </ol>
 */
public class LegacyBitserException extends BitserException {

	/**
	 * See {@link LegacyBitserException}
	 */
	public LegacyBitserException(String message) {
		super(message);
	}
}
