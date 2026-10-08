package org.slowcoders.hyperquery.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface QFromMapper {
    Class<?> mapper() default void.class;

    String sqlId();

    Class<?> parameterType() default void.class;
}
