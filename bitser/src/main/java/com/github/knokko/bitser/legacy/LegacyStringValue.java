package com.github.knokko.bitser.legacy;

/**
 * Instances of this class are returned during the backward-compatible deserialization of string fields. This class
 * is usually not interesting for users, but it may appear in the legacy values of a
 * {@link com.github.knokko.bitser.BitPostInit.Context}.
 *
 * @param value The wrapped string value
 */
public record LegacyStringValue(String value) {

	@Override
	public String toString() {
		return "string " + value;
	}
}
