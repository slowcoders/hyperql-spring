package org.slowcoders.hyperquery.core;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface Q {
    interface Relation extends QRelation {}

    @Retention(RetentionPolicy.RUNTIME)
    @interface From {
        /** condition expression to filtering */
        String value();
    }

}
