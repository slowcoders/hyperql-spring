package org.slowcoders.hyperquery.core;

import org.slowcoders.hyperquery.impl.HFilter;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class QFilter<T extends QEntity<?>> extends HFilter {

    public enum LogicalOp {
        AND, OR, NOT_AND, NOT_OR;
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Predicate {
        /** condition expression to filtering */
        String value() default "";

        boolean bypassEmptyInput() default true;

        String convertInput() default "";

        String inputType() default "";
    }

    @Repeatable(Begin.Stack.class)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Begin {
        LogicalOp value();

        @Retention(RetentionPolicy.RUNTIME)
        @interface Stack {
            Begin[] value();
        }
    }

    @Repeatable(EndOf.Stack.class)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface EndOf {
        LogicalOp value();

        @Retention(RetentionPolicy.RUNTIME)
        @interface Stack {
            EndOf[] value();
        }
    }

    public interface Validator<T> {
        boolean isValid(T value);
    }

}
