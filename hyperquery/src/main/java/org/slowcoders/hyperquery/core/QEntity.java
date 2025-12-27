package org.slowcoders.hyperquery.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class QEntity {
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TColumn {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface PKColumn {
        String value();
    }
}
