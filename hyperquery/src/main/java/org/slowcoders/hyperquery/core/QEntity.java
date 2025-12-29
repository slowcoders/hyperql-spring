package org.slowcoders.hyperquery.core;

import org.slowcoders.hyperquery.impl.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface QEntity {

    @Retention(RetentionPolicy.RUNTIME)
    @interface TColumn {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface PKColumn {
        String value();
    }

    static void init() {}
    class Join extends QJoin {
        private Join(QView view, String joinOn, boolean single) {
            super(view, joinOn, single);
        }

        public static Join toOne(Class<? extends QEntity> targetEntity, String joinOn) {
            return new Join(HSchema.registerSchema(targetEntity), joinOn, true);
        }

        public static Join toMany(Class<? extends QEntity> targetEntity, String joinOn) {
            return new Join(HSchema.registerSchema(targetEntity), joinOn, false);
        }

        public static Join toOne(QView view, String joinOn) {
            return new Join(view, joinOn, true);
        }

        public static Join toMany(QView view, String joinOn) {
            return new Join(view, joinOn, false);
        }
    }

    class Property extends QAttribute {
        private Property(String statement) {
            super(statement);
        }

        public static Property formula(String statement) {
            return new Property(statement);
        }
    }

    class Lambda extends QLambda {
        private Lambda(int argCount, String statement) {
            super(argCount, statement);
        }

        public static Lambda script(int argCount, String statement) {
            return new Lambda(argCount, statement);
        }

        public static Lambda importMapper(Class<? extends QRepository> reps, String mapperName, String[] parameterNames) {
            return new Lambda(0, null);
        }
    }
}
