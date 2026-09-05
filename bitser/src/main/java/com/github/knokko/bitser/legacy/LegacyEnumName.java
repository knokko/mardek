package com.github.knokko.bitser.legacy;

/**
 * Instances of this class are created during the backward-compatible deserialization of an enum field with
 * {@link com.github.knokko.bitser.BitEnum.Mode#Name}. This class is usually not interesting for users, but it may
 * appear in the legacy values of a {@link com.github.knokko.bitser.BitPostInit.Context}.
 *
 * @param name The name of the enum constant, when it was serialized.
 */
public record LegacyEnumName(String name) {

	@Override
	public String toString() {
		return "enum constant " + name;
	}
}
