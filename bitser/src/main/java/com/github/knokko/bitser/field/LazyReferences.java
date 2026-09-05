package com.github.knokko.bitser.field;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is required on all fields of type {@link com.github.knokko.bitser.ReferenceLazyBits}. It states
 * which {@link ReferenceField#label()}s the 'outer' reference fields of the wrapped/lazy object are allowed to have.
 * The 'inner' references can still use any label, regardless of what is specified here. Specifying more labels than
 * needed is allowed, but may increase memory usage.
 */
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface LazyReferences {

	/**
	 * The {@link ReferenceField#label()}s which the 'outer' reference fields of the
	 * wrapped/lazy object are allowed to have.
	 */
	String[] labels();
}
