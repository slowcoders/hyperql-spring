package org.slowcoders.hyperquery.core;

import org.apache.ibatis.reflection.MetaObject;
import org.slowcoders.hyperquery.impl.HCondition;
import org.slowcoders.hyperquery.impl.HCriteria;
import org.slowcoders.hyperquery.impl.PredicateTranslator;
import org.slowcoders.hyperquery.impl.SqlBuilder;
import org.slowcoders.hyperquery.util.SqlWriter;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class PredicateBuilder<T extends QFilter<?>> {
    public static QFilter.Validator<?> _notNull = value -> value != null;
    public static QFilter.Validator _notEmpty = value -> !ObjectUtils.isEmpty(value);
    public static QFilter.Validator _always = value -> true;
    public static QFilter.Validator<?> _mustNotNull = value -> {
        if (value == null) throw new NullPointerException();
        return true;
    };
    public static QFilter.Validator<?> _mustNotEmpty = value -> {
        if (ObjectUtils.isEmpty(value)) throw new NullPointerException();
        return true;
    };
    private SqlBuilder sqlGenerator;

    protected PredicateBuilder(SqlBuilder generator) {
        this.sqlGenerator = generator;
    }

    public abstract HCondition<T> build();

    @SafeVarargs
    public final HCriteria.PredicateSet<T> _AND_(HCondition<T>... conditions) {
        return new HCriteria.PredicateSet<T>(QFilter.LogicalOp.AND, conditions);
    }

    @SafeVarargs
    public final HCriteria.PredicateSet<T> _OR_(HCondition<T>... conditions) {
        return new HCriteria.PredicateSet<T>(QFilter.LogicalOp.OR, conditions);
    }

    public static final class Optional<T extends QFilter<?>> {
        private final PredicateBuilder<T> builder;

        public Optional(PredicateBuilder<T> tPredicateBuilder) {
            this.builder = tPredicateBuilder;
        }

        @SafeVarargs
        public final HCriteria.PredicateSet<T> _OR_(HCondition<T>... conditions) {
            HCriteria.PredicateSet<T> ps = builder._OR_(conditions);
            ps.mustNotEmpty();
            return ps;
        }

        @SafeVarargs
        public final HCriteria.PredicateSet<T> _AND_(HCondition<T>... conditions) {
            HCriteria.PredicateSet<T> ps = builder._AND_(conditions);
            ps.mustNotEmpty();
            return ps;
        }
    }

    public final Optional<T> Optional = new Optional<>(this);

    static class PropertyCheck<T> implements QFilter.Validator<T> {
        String property;
        SqlBuilder sqlGenerator;
        boolean isOptional;

        public PropertyCheck(String property, SqlBuilder sqlGenerator) {
            https://this.property = property.substring(0, property.length() - 1);
            this.sqlGenerator = sqlGenerator;
            this.isOptional = property.endsWith("?");
        }

        @Override
        public boolean isValid(T obj) {
            boolean valid = _notEmpty.isValid(sqlGenerator.getViewResolver().newMetaObject(obj).getValue(property));
            if (!valid && !isOptional) {
                throw new IllegalArgumentException("Property " + property + " is required.");
            }
            return valid;
        }

    }

    public HCriteria.Predicate<T> q(String sql) {
        List<String> paramNames = new ArrayList<>();
        sql = PredicateTranslator.translate(sqlGenerator, paramNames, sql);
        QFilter.Validator<T> validator = new PropertyCheck<T>(paramNames.get(0), sqlGenerator);
        return new HCriteria.Predicate<T>(validator, sql);
    }

    public interface PropertyPicker<T, V> {
        V select(T value);
    }

    public BranchBuilder<T> IF(QFilter.Validator<T> validator, HCondition<T> then) {
        return new BranchBuilder<T>(validator, then);
    }

    public BranchBuilder<T> If(QFilter.Validator<T> validator, HCondition<T> then) {
        return new BranchBuilder<T>(validator, then);
    }

    public BranchBuilder<T> _if_(QFilter.Validator<T> validator, HCondition<T> then) {
        return new BranchBuilder<T>(validator, then);
    }

    public <V> SwitchBuilder<T, V> Switch(PropertyPicker<T, V> picker) {
        return new SwitchBuilder<>(picker);
    }


    public static class SwitchBuilder<T extends QFilter<?>, V> extends HCondition<T> {
        private final PropertyPicker<T, V> valuePicker;
        private final List<HCondition<V>> cases = new ArrayList<>();
        private HCondition<T> finalCondition;

        SwitchBuilder(PropertyPicker<T, V> valuePicker) {
            super(_always);
            this.valuePicker = valuePicker;
        }

        public SwitchBuilder<T, V> When(QFilter.Validator<V> validator, HCondition<T> condition) {
            cases.add(new Conditional<T, V>(validator, condition));
            return this;
        }

        public SwitchBuilder<T, V> When(V value, HCondition<T> condition) {
            cases.add(new Conditional<T, V>(v -> v == null ? value == v : value.equals(v), condition));
            return this;
        }

        public HCondition<T> Otherwise(HCondition<T> condition) {
            this.finalCondition = condition;
            return this;
        }

        public void dump(MetaObject obj, SqlWriter sw) {
            V value = valuePicker.select((T)obj.getOriginalObject());
            for (HCondition<V> c : cases) {
                if (c.isApplicable(value)) {
                    c.dump(obj, sw);
                    return;
                }
            }
            if (finalCondition != null) {
                finalCondition.dump(obj, sw);
            }
        }
    }

    public static class Conditional<T, V> extends HCondition<V> {
        private HCondition<T> _then;

        Conditional(QFilter.Validator<V> validator, HCondition<T> condition) {
            super(validator);
            this._then = condition;
        }
    }

    public static class BranchBuilder<T> extends HCondition<T> {
        private HCondition<T> _then;
        private HCondition<T> _else;

        BranchBuilder(QFilter.Validator<T> validator, HCondition<T> condition) {
            super(validator);
            this._then = condition;
        }

        public BranchBuilder<T> Else_If(QFilter.Validator<T> validator, HCondition<T> condition) {
            BranchBuilder<T> _else = new BranchBuilder<T>(validator, condition);
            this._else = _else;
            return _else;
        }

        public HCondition<T> Else(HCondition<T> condition) {
            _else = condition;
            return this;
        }

        public void dump(MetaObject obj, SqlWriter sw) {
            if (super.isApplicable((T)obj.getOriginalObject())) {
                _then.dump(obj, sw);
            } else if (_else != null) {
                _else.dump(obj, sw);
            }
        }

    }



}
