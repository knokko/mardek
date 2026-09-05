package com.github.knokko.bitser;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     This annotation marks a class as a bit struct. Bitser can only (de)serialize such bit structs.
 * </p>
 *
 * <p>
 *     When bitser serializes a struct, it will only serialize the fields that are annotated with a bitser
 *     annotation (e.g. {@code @BitField} or {@code @IntegerField}).
 * </p>
 *
 * <p>
 *     For deserialization purposes, every struct must have a constructor without parameters. Bitser needs this to
 *     create an 'empty' instance of each struct. All fields annotated with a bitser annotations will subsequently
 *     be replaced with the deserialized field value. For <b>final</b> fields, this constructor will have to assign
 *     <i>some</i> value (Java language rules), but it doesn't really matter <i>which</i> value, since bitser will
 *     overwrite it anyway (using reflection to bypass the <b>final</b> modifier).
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BitStruct {

	/**
	 * <p>
	 *     Whether this struct supports backward-compatible (de)serialization.
	 * </p>
	 * <ul>
	 *     <li>
	 *         When {@code false}, this struct cannot be (de)serialized when the {@link Bitser#BACKWARD_COMPATIBLE} is
	 *         used. When {@link Bitser#BACKWARD_COMPATIBLE} is used, <i>all</i> encountered structs must have
	 *         {@code backwardCompatible = true}.
	 *     </li>
	 *     <li>
	 *         When {@code true}, all fields with at least one bitser annotation must be annotated with
	 *         {@link com.github.knokko.bitser.field.BitField}, and have a non-negative ID specified using
	 *         {@code @BitField(id = x)}. When an old version of a struct is serialized, and a newer version of that
	 *         struct is deserialized from the old data, their fields will be 'matched' by their ID. For instance,
	 *         if the old field with id = 0 was an integer field, an integer will be saved in 'slot 0', and put in
	 *         the new field with id = 0, which is hopefully still an integer field.
	 *     </li>
	 * </ul>
	 */
	boolean backwardCompatible();
}
