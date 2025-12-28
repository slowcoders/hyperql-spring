package org.slowcoders.hyperquery.core;

import org.slowcoders.hyperquery.impl.HqRelation;
import org.slowcoders.hyperquery.impl.QCriteria;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class QFilter<T extends QEntity> {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Condition {
        /** condition expression to filtering */
        String value();
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

    //==============================================================================//

    private String __sql__;
    private final HqRelation relation;


    public QFilter(Class<T> clazz) {
        this.relation = HqRelation.registerRelation(clazz);
    }

    public Object getFromStatement() {
        return this.__sql__;
    }

    public Object getWhereStatement() {
        return this.__sql__;
    }

    public final HqRelation getRelation() {
        return relation;
    }


    /*internal*/ public final void setSql(String sql) {
        this.__sql__ = sql;
    }

    public QCriteria buildCriteria() {
        return QCriteria.buildCriteria(this, "@");
    }


    public String toString() {
        QCriteria criteria = buildCriteria();
        return criteria.toString();
    }
}
