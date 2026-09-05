package com.github.knokko.bitser.legacy;

/**
 * Instances of this class are created during the backward-compatible deserialization of a collection field or array
 * field. This class is usually not interesting for users, but it may appear in the legacy values of a
 * {@link com.github.knokko.bitser.BitPostInit.Context}.
 */
public class LegacyArrayValue {

	/**
	 * The deserialized value. This will always be an array, even if the original value was e.g. an
	 * ArrayList or HashSet. Note that this can be an {@code Object[]}, but also e.g. an {@code int[]}. In all
	 * cases, you can use {@link java.lang.reflect.Array#get(Object, int)} and
	 * {@link java.lang.reflect.Array#getLength(Object)} to examine this array.
	 */
	public final Object array;

	/**
	 * This field should only be used by bitser itself.
	 */
	public Object modernObject;

	/**
	 * This constructor should only be used by bitser itself.
	 */
	public LegacyArrayValue(Object array) {
		this.array = array;
	}
}
