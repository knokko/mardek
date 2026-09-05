package com.github.knokko.bitser.field;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     Marks a field as a <i>class</i> field. This can be used to serialize fields whose exact class is not known, for
 *     instance a field of class {@code Animal}, that could be an instance of either class {@code Dog} or class
 *     {@code Cat}.
 * </p>
 *
 * <p>
 *     In this example, the field should be annotated with {@code @ClassField(root=Animal.class)}, and
 *     {@code Animal.java} should have a {@code static final Class<?>[] BITSER_HIERARCHY = { Cat.class, Dog.class };}.
 * </p>
 *
 * <h3>Backward compatibility</h3>
 * <p>
 *     If the field is backward-compatible, you can add more classes at the <b>end</b> of
 *     {@code BITSER_HIERARCHY}, but you should <b>not</b> change the index of existing classes, since the index is
 *     stored when the field is serialized. For instance, you can change it to {@code BITSER_HIERARCHY =
 *     {Cat.class, Dog.class, Duck.class}}, but <b>not</b>
 *     {@code BITSER_HIERARCHY = { Duck.class, Cat.class, Dog.class}}.
 * </p>
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ClassField {

	/**
	 * The root of the class hierarchy, which must have a {@code static final Class<?>[] BITSER_HIERARCHY}, see
	 * {@link ClassField} for details.
	 */
	Class<?> root();
}
