package com.github.knokko.bitser;

import com.github.knokko.bitser.field.EnumField;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     This field can be used to specify how an enum type should be serialized. This annotation is very
 *     similar to {@link EnumField}, but the difference is that {@code EnumField} should be used on fields, whereas
 *     {@code BitEnum} should be used on enums.
 * </p>
 *
 * <h3>Backward compatibility</h3>
 * <p>
 *     When an enum is backward-compatible, the {@link #mode()} determines whether it is safe to rename or
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
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BitEnum {

	/**
	 * Whether bitser should store the name of the enum constant, or the ordinal
	 */
	Mode mode();

	/**
	 * The <i>mode</i> tells bitser how an enum should be serialized. The options are {@link #Name} and
	 * {@link #Ordinal}.
	 */
	@BitEnum(mode = Mode.Ordinal)
	enum Mode {
		/**
		 * Bitser will serialize constants of this enum by storing their name. This typically takes more space than
		 * {@link #Ordinal}, but allows enum constants to be reordered without breaking backward compatibility.
		 */
		Name,

		/**
		 * Bitser will serialize constants of this enum by storing their ordinal (using a variable-length integer). This
		 * is typically more compact than {@link #Name}, and allows enum constants to be renamed without breaking
		 * backward compatibility. The drawback is that reordering enum constants will break backward compatibility.
		 */
		Ordinal
	}
}
