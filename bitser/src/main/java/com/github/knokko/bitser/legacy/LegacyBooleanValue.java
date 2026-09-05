package com.github.knokko.bitser.legacy;

/**
 * Instances of this class are returned during the backward-compatible deserialization of a boolean field. This class
 * is usually not interesting for users, but it may appear in the legacy values of a
 * {@link com.github.knokko.bitser.BitPostInit.Context}. All instances of this class must  be either {@link #TRUE} or
 * {@link #FALSE}.
 */
public class LegacyBooleanValue {

	public static final LegacyBooleanValue TRUE = new LegacyBooleanValue(true);
	public static final LegacyBooleanValue FALSE = new LegacyBooleanValue(false);

	public static LegacyBooleanValue get(boolean value) {
		return value ? TRUE : FALSE;
	}

	public final boolean value;

	private LegacyBooleanValue(boolean value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return "boolean " + value;
	}
}
