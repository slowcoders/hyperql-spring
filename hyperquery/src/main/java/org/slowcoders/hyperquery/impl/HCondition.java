package org.slowcoders.hyperquery.impl;

import org.apache.ibatis.reflection.MetaObject;
import org.slowcoders.hyperquery.core.QFilter;
import org.slowcoders.hyperquery.util.SqlWriter;

public class HCondition {
    private final QFilter.Validator condition;
    private final HCondition[] conditions;
    private final String predication;

    protected HCondition(QFilter.Validator condition, String predication, HCondition[] conditions) {
        this.condition = condition;
        this.predication = predication;
        this.conditions = conditions;
    }

    protected boolean apply(MetaObject value) {
        return condition.isValid(value.getOriginalObject());
    }

    protected void dump(MetaObject obj, SqlWriter sw) {
        if (apply(obj)) {
            if (predication != null) {
                sw.write(predication);
            } else {
                for (HCondition cond : conditions) {
                    cond.dump(obj, sw);
                }
            }
        }
    }

    final QFilter.Validator getApplicability() {
        return this.condition;
    }
}
