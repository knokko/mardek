package com.github.knokko.bitser.field;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     This annotation can be used to specify how a string field should be serialized.
 * </p>
 *
 * <p>
 *     This annotation is optional when a field is already annotated with {@link BitField}. When this annotation is
 *     omitted, the string will be serialized using the default settings.
 * </p>
 *
 * <p>
 *     Currently, only the {@link #length()} setting can be changed, but bitser may support more settings in
 *     the future.
 * </p>
 *
 * <h3>Backward compatibility</h3>
 * <p>
 *     If a field of the original serialized struct is a string field, the corresponding field of the new deserialized
 *     struct must also be a string field. (It doesn't matter whether either of the fields are explicitly annotated
 *     with {@code @StringField}, or only with {@code @BitField}.)
 * </p>
 * <p>
 *     Changing the {@link #length()} restrictions is allowed, as long as all serialized values satisfy the
 *     length restrictions of the new deserialized field.
 * </p>
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StringField {

	/**
	 * <p>
	 *     This setting determines the minimum/maximum length of the strings, as well as the way it is serialized.
	 * </p>
	 *
	 * <p>
	 *     For instance, if the length of the strings are always between 5 and 10 bytes, you can annotate the field
	 *     with {@code @StringField(length = @IntegerField(expectUniform = true, minValue = 5, maxValue = 8))}. In
	 *     this example, bitser would always use 3 bits to store the length of each string.
	 * </p>
	 *
	 * <p>
	 *     When the length of a string value (in bytes) is outside this range, an
	 *     {@link com.github.knokko.bitser.exceptions.InvalidBitValueException} will be thrown.
	 * </p>
	 */
	IntegerField length() default @IntegerField(expectUniform = false, minValue = 0, maxValue = Integer.MAX_VALUE);
}
