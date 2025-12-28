package org.slowcoders.hyperquery.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface QRecord<T extends QEntity> {
    @Retention(RetentionPolicy.RUNTIME)
    @interface Property {
        String value();
    }
}
