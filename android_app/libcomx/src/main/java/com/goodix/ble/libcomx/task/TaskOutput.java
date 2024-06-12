package com.goodix.ble.libcomx.task;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Caution: Do not apply it on the primitive type or the primitive wrapper class.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface TaskOutput {
    boolean skippable() default false;
}
