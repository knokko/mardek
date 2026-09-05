package com.github.knokko.bitser.field;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     Use this annotation to mark a field of a {@link com.github.knokko.bitser.BitStruct} as a <i>BitField</i>,
 *     which causes bitser to (de)serialize it.
 * </p>
 *
 * <p>
 *     This annotation can also be used to mark fields as {@link #optional()} (nullable), or to define their ID.
 *     The latter is required for all <i>BitFields</i> of <b>backward-compatible</b> structs.
 * </p>
 *
 * <p>
 *     Note that this annotation is optional for some fields of non-backward-compatible structs: when a field has
 *     another bitser annotation (e.g. {@link IntegerField}), but no <i>BitField</i>, bitser will treat it like it has
 *     {@code @BitField(optional = false)}.
 * </p>
 *
 * <p>
 *     Fields without any bitser annotations will be ignored by bitser.
 * </p>
 *
 * <h3>Backward compatibility</h3>
 * <h4>IDs</h4>
 * <p>
 *     For backward-compatible structs, this annotation is <b>required</b> on all fields that should be serialized, and
 *     you must assign each serializable field an ID, e.g.
 *     {@code @BitField(id=0) @IntegerField(expectUniform=false) private int x;}.
 * </p>
 * <p>
 *     Bitser will assume that the field with id X on the original serialized struct represents the field with id X
 *     on the new deserialized struct. This means that you can freely rename fields of backward-compatible structs, but
 *     that you must <b>not</b> change the IDs of fields, since that could lead to data loss or incompatibilities.
 * </p>
 * <p>
 *     When the original serialized struct has a field with id X, but the new deserialized struct does not, the value
 *     of that field will be discarded. Note however that the new deserialized struct can still see it if it implements
 *     {@link com.github.knokko.bitser.BitPostInit}.
 * </p>
 * <p>
 *     When the new deserialized struct has a field with id X, but the old serialized struct does not, the new field
 *     will be initialized only by the default/zero-parameter constructor of the new deserialized struct. Also in this
 *     case, the new the struct can implement {@link com.github.knokko.bitser.BitPostInit} to assign a better value to
 *     the new field.
 * </p>
 *
 * <h4>Optional</h4>
 * <p>
 *     When a field of the old serialized struct is optional, but the corresponding field of the new deserialized
 *     struct is not, an exception may or may not be thrown:
 * </p>
 * <ul>
 *     <li>
 *         If the serialized field <i>value</i> is null,
 *         a {@link com.github.knokko.bitser.exceptions.LegacyBitserException} will be thrown, since the new field does
 *         <b>not</b> accept null.
 *     </li>
 *     <li>
 *         If the serialized field <i>value</i> is <b>not</b> null, the new struct will be deserialized as usual.
 *     </li>
 * </ul>
 * <p>
 *     This means that you can remove the {@link #optional()} from a field, but only if the values weren't actually
 *     null.
 * </p>
 * <p>
 *     When a field of the new deserialized struct is optional, but the corresponding field of the old serialized struct
 *     is not, the new struct will be deserialized as usual, since all valid values of the original field are also
 *     valid for the new field.
 * </p>
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BitField {

	/**
	 * The ID of the field, which is needed for backward-compatible structs.
	 * When serializing a field with {@link com.github.knokko.bitser.Bitser#BACKWARD_COMPATIBLE},
	 * the ID of each serialized field is stored alongside the serialized value. Upon deserializing, each serialized
	 * value is assigned to the field with the corresponding serialized ID.
	 */
	int id() default -1;

	/**
	 * Whether this field is allowed to be {@code null}
	 */
	boolean optional() default false;

	/**
	 * <p>
	 *     Whether this field should read its value from a <b>method</b> that is annotated with
	 *     {@code @BitField(id = sameId)}.
	 * </p>
	 *
	 * <p>
	 *     This is {@code false} by default, and should almost always be {@code false}.
	 *     Choosing {@code true} is only allowed if:
	 * </p>
	 *
	 * <ul>
	 *     <li>This {@code @BitField} annotation is put on a <b>field</b>, and</li>
	 *     <li>the {@link #id()} is non-negative, and</li>
	 *     <li>
	 *         the struct containing this field has a <b>method</b> that is annotated with
	 *         {@code @BitField(id = sameIdAsThisField)}.
	 *     </li>
	 * </ul>
	 *
	 * <p>
	 *     When this property is {@code true}, this field will never be serialized. When this field is
	 *     <b>de</b>serialized, it will take the result of the method that is annotated with
	 *     {@code @BitField(id = sameIdAsThisField)}.
	 * </p>
	 *
	 * <p>
	 *     You can use this to e.g. derive the value of a field from other fields, and it allows you to control the
	 *     serialized values using <i>with</i> parameters.
	 * </p>
	 */
	boolean readsMethodResult() default false;
}
