package org.slowcoders.hyperquery.core;

import org.slowcoders.hyperquery.impl.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class QEntity extends QView {

    @Retention(RetentionPolicy.RUNTIME)
    @interface TColumn {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface PKColumn {
        String value();
    }
}
