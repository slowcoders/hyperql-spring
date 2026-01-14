package org.slowcoders.hyperquery.core;

public interface QRecord<T extends QRecord<T>> {

//    @Retention(RetentionPolicy.RUNTIME)
//    @interface Property {
//        String value();
//    }

    public static class Join extends QJoin {
        private Join(QInlineView view, String joinOn, boolean single) {
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

        public static Join toOne(QInlineView view, String joinOn) {
            return new Join(view, joinOn, true);
        }

        public static Join toMany(QInlineView view, String joinOn) {
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
}
