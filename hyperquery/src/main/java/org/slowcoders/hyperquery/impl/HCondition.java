package org.slowcoders.hyperquery.impl;

import org.apache.ibatis.reflection.MetaObject;
import org.slowcoders.hyperquery.core.QFilter;
import org.slowcoders.hyperquery.util.SqlWriter;

public class HCondition<V> {
    private final QFilter.Validator<V> validator;

    protected HCondition(QFilter.Validator<V> validator) {
        this.validator = validator != null ? validator : (QFilter.Validator<V>) (o) -> true;
    }

    public boolean isApplicable(V value) {
        return validator.isValid(value);
    }

    public void dump(MetaObject record, SqlWriter sw) {
    }

}
