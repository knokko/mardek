package com.github.knokko.bitser.field;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The <i>target</i> of a {@link ReferenceField}. Check the doc comments of {@link ReferenceField} for details.
 *
 * <h3>Backward compatibility</h3>
 * There are no special backward compatibility rules for reference targets: you can (un)mark fields as reference targets
 * whenever you want.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReferenceFieldTarget {

	/**
	 * This reference target can only be used by reference fields with the same {@link ReferenceField#label()}.
	 */
	String label();
}
