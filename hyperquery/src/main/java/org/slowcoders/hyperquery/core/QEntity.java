package org.slowcoders.hyperquery.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface QEntity<T extends QEntity<T>> extends QUniqueRecord<T> {

    @Retention(RetentionPolicy.RUNTIME)
    @interface TColumn {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface PKColumn {
        String value();
    }
}
