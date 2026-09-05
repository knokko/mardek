package com.github.knokko.bitser.legacy;

/**
 * Instances of this class are created during the backward-compatible deserialization of an enum field with
 * {@link com.github.knokko.bitser.BitEnum.Mode#Ordinal}. This class is usually not interesting for users, but it may
 * appear in the legacy values of a {@link com.github.knokko.bitser.BitPostInit.Context}.
 *
 * @param ordinal The ordinal of the enum constant, when it was serialized.
 */
public record LegacyEnumOrdinal(int ordinal) {

	@Override
	public String toString() {
		return "enum ordinal " + ordinal;
	}
}
