package com.github.knokko.bitser.field;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation makes {@link NestedFieldSetting} repeatable. You should normally <b>not</b> use this annotation
 * directly. Instead, you can annotate a collection field with multiple {@link NestedFieldSetting}s.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface NestedFieldSettings {

    NestedFieldSetting[] value();
}
