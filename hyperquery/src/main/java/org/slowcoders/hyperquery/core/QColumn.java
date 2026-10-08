package org.slowcoders.hyperquery.core;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface QColumn {
    String name();
    String writeTransform() default "?";
    String readTransform() default "?";
}
