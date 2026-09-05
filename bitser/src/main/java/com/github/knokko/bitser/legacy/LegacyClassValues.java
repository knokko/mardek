package com.github.knokko.bitser.legacy;

/**
 * Holds the deserialized values of all the bit fields of a class instance, as well as the deserialized results of all
 * bit method results. Each {@link LegacyStructInstance} contains 1 {@link LegacyClassValues} for each class in its
 * class hierarchy.
 */
public class LegacyClassValues {

	/**
	 * <p>
	 *     The deserialized field values and method results. The value for the field annotated with
	 *     {@code @BitField(id = x)} is stored at index {@code x} into this array, except when it is overwritten by the
	 *     result of a <b>method</b> annotated with {@code @BitField(id = x)}.
	 * </p>
	 *
	 * <p>
	 *     The length of this array will be sufficiently large to store all non-null field values and all non-null
	 *     method results. Any field value with {@code id >= fieldValues.length} or method result with
	 *     {@code id >= fieldValues.length} was either {@code null}, or not present at all.
	 * </p>
	 */
	public final Object[] values;

	/**
	 * For each index {@code i} in {@link #values}, {@code hasValues[i]} denotes whether there was anything
	 * in {@code values[i]}. You can use {@code hasValues} to distinguish between {@code null} and no value
	 * at all:
	 * <ul>
	 *     <li>
	 *         When the field was annotated with {@code @BitField(id = 0, optional = true)} and its value was
	 *         {@code null}, then {@code hasValues[0]} will be {@code true}.
	 *     </li>
	 *     <li>
	 *         When not a single field or method was annotated with {@code @BitField(id = 0)}, then
	 *         {@code hasValues[0]} will be {@code false}.
	 *     </li>
	 * </ul>
	 * This array will always have the same length as {@link #values}, and will be sufficiently large to store all
	 * {@code true} values. During serialization, not a single field had {@code id >= hasFieldValues.length}.
	 */
	public final boolean[] hasValues;

	/**
	 * This constructor should only be used by bitser internally.
	 */
	public LegacyClassValues(int largestFieldOrFunctionID) {
		this.values = new Object[largestFieldOrFunctionID + 1];
		this.hasValues = new boolean[largestFieldOrFunctionID + 1];
	}
}
