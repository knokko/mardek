package com.github.knokko.bitser.field;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 *     All <i>targets</i> of <b>stable</b> {@link ReferenceField}s must have exactly 1 field that is annotated with
 *     {@code @StableReferenceFieldId}.
 * </p>
 *
 * <p>
 *     Currently, only {@link java.util.UUID} fields can be annotated, but bitser might support more types someday.
 * </p>
 *
 * <h3>Backward compatibility</h3>
 *
 * <p>
 *     There are no special backward compatibility rules for stable IDs. You can (un)mark UUID fields as stable ID
 *     whenever you want.
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StableReferenceFieldId {}
