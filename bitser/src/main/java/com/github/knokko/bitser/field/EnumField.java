package com.github.knokko.bitser.field;

import com.github.knokko.bitser.BitEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     This field can be used to specify how a field with an enum type should be serialized. This annotation is very
 *     similar to {@link BitEnum}, but the difference is that {@code EnumField} should be used on fields, whereas
 *     {@code BitEnum} should be used on enums. {@code EnumField} is redundant when the enum type of that field is
 *     annotated with {@code @BitEnum}. {@code EnumField} is useful when either:
 * </p>
 * <ul>
 *     <li>
 *         The enum is <b>not</b> annotated with {@code @BitEnum}, for instance because you don't control it.
 *     </li>
 *     <li>
 *         The enum is annotated with {@code @BitEnum}, but you want to override its {@link BitEnum#mode()}.
 *         If both {@code EnumField} and {@code BitEnum} are present, {@code EnumField} will overrule {@code BitEnum}.
 *     </li>
 * </ul>
 *
 * <h3>Backward compatibility</h3>
 * <p>
 *     When an enum field is backward-compatible, the {@link #mode()} determines whether it is safe to rename or
 *     reorder the enum constants:
 * </p>
 * <ul>
 *     <li>
 *         When the mode is {@link BitEnum.Mode#Name}, reordering the enum constants is totally safe, but they must
 *         <b>not</b> be renamed. New enum constants (with new names) can be added safely.
 *     </li>
 *     <li>
 *         When the mode is {@link BitEnum.Mode#Ordinal}, renaming the enum constants is totally safe, but they must
 *         <b>not</b> be reordered. New enum constants should only be added to the end of the enum. For instance,
 *         you can safely change {@code enum Animal { Cat, Dog }} to {@code enum Animal { Cat, Dog, Duck }}, but you
 *         should <b>not</b> change it to {@code enum Animal { Duck, Cat, Dog }}.
 *     </li>
 * </ul>
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumField {

	/**
	 * Whether bitser should store the name of the enum constant, or the ordinal
	 */
	BitEnum.Mode mode();
}
