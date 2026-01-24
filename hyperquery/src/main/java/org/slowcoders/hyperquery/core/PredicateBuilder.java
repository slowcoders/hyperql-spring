package org.slowcoders.hyperquery.core;

import org.slowcoders.hyperquery.impl.HCondition;
import org.slowcoders.hyperquery.impl.HCriteria;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class PredicateBuilder<T extends QFilter<?>>  {
    public static QFilter.Validator<?> _notNull = value -> value != null;
    public static QFilter.Validator<?> _notEmpty = value -> !ObjectUtils.isEmpty(value);
    public static QFilter.Validator<?> _always = value -> true;
    public static QFilter.Validator<?> _mustNotNull = value -> {
        if (value == null) throw new NullPointerException();
        return true;
    };
    public static QFilter.Validator<?> _mustNotEmpty = value -> {
        if (ObjectUtils.isEmpty(value)) throw new NullPointerException();
        return true;
    };

//    public PredicateBuilder(QFilter.LogicalOp op, QFilter.Availability availability) {
//        super(availability, null, null);
//    }

    public abstract HCondition build();

    public PredicateSet<T> PredicateSet(QFilter.LogicalOp op) {
        return new PredicateSet(op);
    }

    public CaseBuilder<T, Object> PropertyCase(String propertyName, QFilter.Validator<?> validator) {
        return new CaseBuilder<T, Object>(propertyName, (QFilter.Validator<Object>)validator);
    }

    public <V> CaseBuilder<T, V> GeneralCase(Compute<T, V> convertor, QFilter.Validator<?> validator) {
        return new CaseBuilder<T, V>(null, (QFilter.Validator<V>)validator);
    }

    public interface Compute<IN, OUT> {
        OUT compute(IN filter);
    }

    public static class PredicateSet<T> extends HCondition {
        private final QFilter.LogicalOp op;
        protected List<HCondition> conditions = new ArrayList<>();
        private boolean mustNotEmpty;


        public PredicateSet(QFilter.LogicalOp op) {
            super(_always, null, null);
            this.op = op;
        }

        public PredicateSet<T> mustNotEmpty() {
            this.mustNotEmpty = true;
            return this;
        }

        public PredicateSet<T> add(HCondition applicability) {
            conditions.add(applicability);
            return this;
        }

        public PredicateSet<T> add(String sql, String property, QFilter.Validator validator) {
            return add(new HCriteria.Predicate(property, validator, sql, null));
        }

    }


    public static class CaseBuilder<T extends QFilter<?>, V> extends PredicateSet<T> {
        private final String property;
        private final QFilter.Validator<V> validator;

        CaseBuilder(String property, QFilter.Validator<V> validator) {
            super(QFilter.LogicalOp.OR);
            this.property = property;
            this.validator = validator;
        }

        public CaseBuilder<T, V> when(QFilter.Validator<V> validator, String then) {
            conditions.add(new HCriteria.Predicate(property, validator, then, null));
            return this;
        }

        public CaseBuilder<T, V> when(QFilter.Validator<V> validator, HCondition condition) {
            conditions.add(new HCriteria.Predicate(property, validator, null, new HCondition[]{condition}));
            return this;
        }
        public CaseBuilder<T, V> equals(V value, String predication) {
            conditions.add(new HCriteria.Predicate(property, (v) -> v == value, predication, null));
            return this;
        }

        public CaseBuilder<T, V> equals(V value, HCondition condition) {
            conditions.add(new HCriteria.Predicate(property, (v) -> v == value, null, new HCondition[] {condition} ));
            return this;
        }

        public CaseBuilder<T, V> otherwise(String sql) {
            conditions.add(new HCriteria.Predicate(property, _always, sql, null));
            return this;
        }

        public CaseBuilder<T, V> otherwise(HCondition condition) {
            conditions.add(new HCriteria.Predicate(property, _always, null, new HCondition[] {condition} ));
            return this;
        }
    }



}
