package org.slowcoders.hyperquery.core;

import org.slowcoders.hyperquery.impl.HModel;

public interface QRecord<T extends QEntity<T>> {

//    @Retention(RetentionPolicy.RUNTIME)
//    @interface Property {
//        String value();
//    }

    public static class Join extends QJoin {
        private Join(HModel view, String joinOn, boolean single) {
            super(view, joinOn, single);
        }
        private Join(Class<? extends QEntity<?>> recordType, String joinOn, boolean single) {
            super(recordType, joinOn, single);
        }

        public static Join toOne(Class<? extends QEntity<?>> recordType, String joinOn) {
            return new Join(recordType, joinOn, true);
        }

        public static Join toMany(Class<? extends QEntity<?>> recordType, String joinOn) {
            return new Join(recordType, joinOn, false);
        }

        public static Join toOne(HModel view, String joinOn) {
            return new Join(view, joinOn, true);
        }

        public static Join toMany(HModel view, String joinOn) {
            return new Join(view, joinOn, false);
        }
    }

    public static class Property extends QAttribute {
        private Property(String statement) {
            super(statement);
        }

        public static Property formula(String statement) {
            return new Property(statement);
        }
    }

    public static class Lambda extends QLambda {
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

    static final ThreadLocal<String> _sql = new ThreadLocal<>();
    static final ThreadLocal<Object> _session = new ThreadLocal<>();
    default String get__sql__() { return _sql.get(); }
    default Object get__session__() { return _session.get(); }

}
