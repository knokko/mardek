package com.github.knokko.bitser.legacy;

/**
 * Instances of this class are created during the backward-compatible deserialization of
 * {@link com.github.knokko.bitser.field.FloatField}s. This class is usually not interesting for users, but it may
 * appear in the legacy values of a {@link com.github.knokko.bitser.BitPostInit.Context}.
 *
 * @param value The deserialized value, converted to a {@code double}
 */
public record LegacyFloatValue(double value) {

	@Override
	public String toString() {
		return "floating " + value;
	}
}
