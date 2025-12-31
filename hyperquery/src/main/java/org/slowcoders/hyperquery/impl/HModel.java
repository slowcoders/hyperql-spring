package org.slowcoders.hyperquery.impl;

import org.slowcoders.hyperquery.core.QJoin;

public abstract class HModel {

    protected abstract HSchema loadSchema();

    public void initialize() {
    }

    public abstract String getQuery();

    public abstract String getTableName();

    public String translateProperty(String property) {
        return property;
    }

    public QJoin getJoin(String alias) {
        return null;
    }

    public QLambda getLambda(String alias) {
        return null;
    }
}
