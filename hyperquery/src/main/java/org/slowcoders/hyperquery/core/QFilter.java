package org.slowcoders.hyperquery.core;

import org.slowcoders.hyperquery.impl.QCriteria;
import org.slowcoders.hyperquery.impl.SqlBuilder;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class QFilter<T extends QEntity<?>> {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Predicate {
        /** condition expression to filtering */
        String value();

        boolean bypassEmptyInput() default true;

        String convertInput() default "";

        String inputType() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface EmbedFilter {
        /** alias of the joined table or view */
        String value();
    }

    @Repeatable(Begin.Stack.class)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Begin {
        QCriteria.LogicalOp value();

        @Retention(RetentionPolicy.RUNTIME)
        @interface Stack {
            Begin[] value();
        }
    }

    @Repeatable(EndOf.Stack.class)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface EndOf {
        QCriteria.LogicalOp value();

        @Retention(RetentionPolicy.RUNTIME)
        @interface Stack {
            EndOf[] value();
        }
    }

}
