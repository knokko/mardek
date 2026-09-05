package com.github.knokko.bitser.field;

import com.github.knokko.bitser.BitStruct;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     This annotation must be used to specify how a floating-point field should be serialized. The floating-point types
 *     supported by bitser are {@code float}, {@code double}, {@link Float}, and {@link Double}.
 * </p>
 *
 * <p>
 *     If you annotate your field using only {@code @FloatField()}, the full 32 or 64 bits will be stored (depending
 *     on whether it's a {@code float} or {@code double}). To make this more compact, you can try
 *     {@link #expectMultipleOf()} and {@link #commonValues()}. You can use
 *     {@link com.github.knokko.bitser.distributions.FloatDistributionTracker} to figure out the most effective
 *     settings.
 * </p>
 *
 * <h3>Backward compatibility</h3>
 * <p>
 *     If a field of the original serialized struct is a {@code FloatField}, the corresponding field of the new
 *     deserialized struct must also be a {@code FloatField}. (At least, if a corresponding field exists. If not, the
 *     old value is simply discarded.)
 * </p>
 * <p>
 *     You can safely change the <i>type</i> of the field, but you will lose precision if you change it from
 *     {@code double} to {@code float}.
 * </p>
 * <p>
 *     You can safely change the properties (e.g. {@link #expectMultipleOf()}), without any consequences.
 * </p>
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FloatField {

	/**
	 * <p>
	 *     In many cases, floating-point fields are often integer multiples of some fixed value like 0.1 or 0.01.
	 *     In such cases, you can optimize the storage by annotating the field with
	 *     {@code @FloatField(expectMultipleOf=0.1)}.
	 * </p>
	 * <p>
	 *     When the value of the field is <i>almost</i> an integer multiple of the {@code expectMultipleOf},
	 *     the integer factor will be stored instead of the full floating-point number, which can save space.
	 *     The {@link #errorTolerance()} determines whether a value is <i>almost</i> a multiple.
	 *     The {@link #expectedIntegerMultiple()} field determines how this integer value is stored. Customizing this
	 *     can sometimes make the storage even more compact.
	 * </p>
	 * <p>
	 *     Note however that specifying an {@code expectMultipleOf} will also add an extra bit to every value,
	 *     including the ones that are <b>not</b> integer multiples (so these would cost 33 or 65 bits). The advantage
	 *     of this extra bit is that non-integer-multiples of {@code expectMultipleOf} can still be serialized.
	 *     The drawback is that, if not enough values are integer multiples, the average number of bits will
	 *     increase rater than decrease.
	 * </p>
	 */
	double expectMultipleOf() default 0.0;

	/**
	 * When {@link #expectMultipleOf()} or {@link #commonValues()} are specified, the {@code errorTolerance} determines
	 * which values are close enough to be considered an integer multiple or common value.
	 * <ul>
	 *     <li>
	 *         A candidate floating-point value {@code x} is considered to be an integer multiple if and only if there
	 *         exists an integer {@code k} such that {@code abs(k * expectMultipleOf - x) <= errorTolerance}.
	 *     </li>
	 *     <li>
	 *         A candidate floating-point value {@code x} is considered 'equal' to a common value {@code c} if and only
	 *         if {@code abs(x - c) <= errorTolerance}.
	 *     </li>
	 * </ul>
	 */
	double errorTolerance() default 0.001;

	/**
	 * <p>
	 *     When the serialized value is an integer multiple of {@link #expectMultipleOf()}, this field determines how
	 *     that integer multiple will be stored. By default, it is a variable integer without upper bound or lower
	 *     bound, which is often reasonable, but not always optimal. You can try to optimize the storage space by
	 *     tuning this field.
	 * </p>
	 *
	 * <p>
	 *     If you assign a {@link IntegerField#minValue()} or {@link IntegerField#maxValue()}, and a value above
	 *     {@code maxValue * expectMultipleOf} or below {@code minValue * expectMultipleOf} is serialized, that value
	 *     will be stored as float instead (using the full 33 or 65 bits). This means that you can choose any
	 *     `minValue` or `maxValue`, although the storage space may increase rather than decrease if too many values
	 *     fall outside the bounds.
	 * </p>
	 */
	IntegerField expectedIntegerMultiple() default @IntegerField(expectUniform = false);

	/**
	 * <p>
	 *     For many fields, certain values are much more common than others. For instance, the values 0.0 and 1.0
	 *     probably occur much more often than e.g. 12.3. In such cases, you can specify `commonValues = [0.0, 1.0]` to
	 *     optimize the used storage space.
	 * </p>
	 *
	 * <p>
	 *     If you specify at least common value, bitser will add 1 bit before each serialized value, which indicates
	 *     whether the serialized value is one of the `commonValues`. If so, the (integer) index of that value into
	 *     `commonValues` is serialized. Otherwise, the bit is followed by the full 32 or 64 bits to store the float
	 *     value. So, serializing one of the common values takes only a couple of bits, which can save lots of storage
	 *     space.
	 * </p>
	 *
	 * <p>
	 *     The {@link #errorTolerance()} determines whether a candidate value to be serialized is close enough to one
	 *     of the `commonValues`.
	 * </p>
	 */
	double[] commonValues() default {};

	@BitStruct(backwardCompatible = false)
	class Properties {

		@FloatField(errorTolerance = 0.0, commonValues = { 0.0, 0.01, 0.05, 0.1, 0.5, 1.0 })
		public final double expectMultipleOf;

		@FloatField(errorTolerance = 0.0, commonValues = { 0.0, 0.001, 0.01 })
		public final double errorTolerance;

		@BitField
		public final IntegerField.Properties expectedIntegerMultiple;

		@FloatField(commonValues = { 0.0, 1.0 })
		public final double[] commonValues;

		public Properties(
				double expectMultipleOf, double errorTolerance,
				IntegerField.Properties expectedIntegerMultiple, double[] commonValues
		) {
			this.expectMultipleOf = expectMultipleOf;
			this.errorTolerance = errorTolerance;
			this.expectedIntegerMultiple = expectedIntegerMultiple;
			this.commonValues = commonValues;
		}

		public Properties(FloatField field) {
			this(
					field.expectMultipleOf(), field.errorTolerance(),
					new IntegerField.Properties(field.expectedIntegerMultiple()),
					field.commonValues()
			);
		}

		public Properties() {
			this(0.0, 0.0, new IntegerField.Properties(), new double[0]);
		}
	}
}
