package com.github.knokko.bitser.field;

import com.github.knokko.bitser.BitStruct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     This annotation is required to specify how an integer field should be serialized. Note that the type of the
 *     field does not necessarily have to be <b>int</b>: it can also be another integer type like <b>long</b> or
 *     {@link Short}.
 * </p>
 * <p>
 *     The easiest way to annotate an integer field is by using either {@code @IntegerField(expectUniform=false)} or
 *     {@code @IntegerField(expectUniform=true)}, but bitser supports a couple of more properties to potentially save
 *     storage space. You can use {@link com.github.knokko.bitser.distributions.IntegerDistributionTracker} to figure
 *     out the most effective settings.
 * </p>
 *
 * <h3>Backward compatibility</h3>
 * <p>
 *     If a field of the original serialized struct is an {@code IntegerField}, the corresponding field of the new
 *     deserialized struct must also be an {@code IntegerField}. (At least, if a corresponding field exists. If not, the
 *     old value is simply discarded.)
 * </p>
 * <p>
 *     You are allowed to change the <i>type</i> of the field, but only if the new type can represent all the values
 *     that were serialized. (For instance, changing the type from <b>int</b> to <b>byte</b> is only allowed if all
 *     serialized values were in the range `[Byte.MIN_VALUE, Byte.MAX_VALUE]`. Changing the type to a larger type
 *     (e.g. <b>int</b> to <b>long</b>) is always allowed.
 * </p>
 * <p>
 *     Likewise, you are allowed to change {@link #minValue()} and {@link #maxValue()}, but only if all the original
 *     serialized values were in the range `[minValue, maxValue]`.
 * </p>
 * <p>
 *     You can safely change the other properties: {@link #expectUniform()}, {@link #digitSize()},
 *     and {@link #commonValues()}
 * </p>
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IntegerField {

	/**
	 * The 'magic' value of {@link #digitSize()} to indicate that bitser should use its terminator-bit strategy instead
	 * of a digit-based serialization strategy. This is also the default value of {@link #digitSize()}.
	 */
	int DIGIT_SIZE_TERMINATORS = 0;

	/**
	 * <p>
	 *     The minimum value this field is allowed to have. An
	 *     {@link com.github.knokko.bitser.exceptions.InvalidBitValueException} will be thrown when an attempt is made
	 *     to serialize a smaller value.
	 * </p>
	 *
	 * <p>
	 *     The default {@code minValue} is the minimum value of the type of this field (e.g. {@code Long.MIN_VALUE} for
	 *     <b>long</b> fields and {@code Integer.MIN_VALUE} for <b>int</b> fields.
	 * </p>
	 *
	 * <p>
	 *     The minimum and maximum value affect the number of bits that is needed to store values of integer fields,
	 *     especially when {@link #expectUniform()} is true.
	 * </p>
	 */
	long minValue() default Long.MIN_VALUE;

	/**
	 * <p>
	 *     The maximum value this field is allowed to have. An
	 *     {@link com.github.knokko.bitser.exceptions.InvalidBitValueException} will be thrown when an attempt is made
	 *     to serialize a larger value.
	 * </p>
	 *
	 * <p>
	 *     The default {@code maxValue} is the maximum value of the type of this field (e.g. {@code Long.MAX_VALUE} for
	 *     <b>long</b> fields and {@code Integer.MAX_VALUE} for <b>int</b> fields.
	 * </p>
	 *
	 * <p>
	 *     The minimum and maximum value affect the number of bits that is needed to store values of integer fields,
	 *     especially when {@link #expectUniform()} is true.
	 * </p>
	 */
	long maxValue() default Long.MAX_VALUE;

	/**
	 * <p>
	 *     Whether the values of this field are expected to be (approximately) uniformly distributed between
	 *     {@link #minValue()} and {@link #maxValue()}.
	 * </p>
	 *
	 * <ul>
	 *     <li>
	 *         When {@code expectUniform = true}, each value of this field will be serialized using
	 *         {@code ceil(log2(1 + maxValue - minValue))} bits.
	 *     </li>
	 *     <li>
	 *         When {@code expectUniform = false}, the {@link #digitSize()} determines how each value is saved.
	 *     </li>
	 * </ul>
	 */
	boolean expectUniform();

	/**
	 * <p>
	 *     When {@link #expectUniform()} is false, the {@code digitSize} determines how each value is serialized,
	 *     and how many bits each value takes. Values closer to 0 will use fewer bits than values further away from 0,
	 *     which is beneficial when values closer to 0 are much more common.
	 *     When {@link #expectUniform()} is true, the {@code digitSize} is ignored.
	 * </p>
	 *
	 * <p>
	 *     When {@code digitSize = DIGIT_SIZE_TERMINATORS} (the <b>default</b>), each field value will be saved
	 *     using a variable-length integer encoding based on so-called 'terminator bits':
	 *     {@link com.github.knokko.bitser.IntegerBitser#encodeVariableIntegerUsingTerminatorBits}
	 * </p>
	 *
	 * <p>
	 *     Otherwise, {@code digitSize} must be a number between 2 and 7 (both inclusive). Each value will be serialized
	 *     using a variable-length integer encoding based on digits (just like humans write numbers):
	 *     {@link com.github.knokko.bitser.IntegerBitser#encodeDigitInteger}
	 * </p>
	 */
	int digitSize() default DIGIT_SIZE_TERMINATORS;

	/**
	 * <p>
	 *     The {@code commonValues} can be used to specify frequently-occurring values. Bitser will ensure that saving
	 *     these values takes fewer bits than saving other values.
	 * </p>
	 *
	 * <p>
	 *     For instance, when {@code commonValues = [50, 100]}, saving 50 or 100 will take only 2 bits,
	 *     whereas saving 49 or 99 will take considerably more bits (the exact amount determines on e.g.
	 *     {@link #maxValue()} and {@link #expectUniform()}).
	 * </p>
	 *
	 * <p>
	 *     The drawback of using {@code commonValues} is that saving any non-common value will take an extra bit when
	 *     at least one common value is specified. Specifying multiple common values increases the number of bits
	 *     needed to store common values: saving a common value takes {@code 1 + ceil(log2(commonValues.length))} bits.
	 * </p>
	 *
	 * <p>
	 *     Note that smaller values (closer to 0) will take fewer bits to save when {@link #expectUniform()} is false,
	 *     so specifying small common values is not always effective, since small values are already rather cheap to
	 *     store.
	 * </p>
	 */
	long[] commonValues() default {};

	@BitStruct(backwardCompatible = false)
	class Properties {

		@IntegerField(expectUniform = false, commonValues = {
				0L, -1L, Byte.MIN_VALUE, Short.MIN_VALUE, Integer.MIN_VALUE, Long.MIN_VALUE
		})
		public final long minValue;

		@IntegerField(expectUniform = true, commonValues = {
				Byte.MAX_VALUE, Short.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE
		})
		public final long maxValue;

		@BitField
		public final boolean expectUniform;

		@BitField
		@IntegerField(expectUniform = true, minValue = 0, maxValue = 7)
		public final int digitSize;

		@BitField
		@IntegerField(expectUniform = true)
		public final long[] commonValues;

		public Properties(long minValue, long maxValue, boolean expectUniform, int digitSize, long[] commonValues) {
			this.minValue = minValue;
			this.maxValue = maxValue;
			this.expectUniform = expectUniform;
			this.commonValues = commonValues;
			this.digitSize = digitSize;
		}

		public Properties(IntegerField field) {
			this(field.minValue(), field.maxValue(), field.expectUniform(), field.digitSize(), field.commonValues());
		}

		public Properties() {
			this(0, 0, false, 0, new long[0]);
		}
	}
}
