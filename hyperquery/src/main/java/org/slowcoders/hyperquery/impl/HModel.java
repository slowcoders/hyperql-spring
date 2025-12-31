package org.slowcoders.hyperquery.impl;

import org.slowcoders.hyperquery.core.QJoin;

public abstract class HModel {

    public void initialize() {
    }

    protected abstract HSchema loadSchema();

    protected abstract String getQuery();

    protected abstract String getTableName();

    protected String translateProperty(String property) {
        return property;
    }

    protected QJoin getJoin(String alias) {
        return null;
    }

    protected QLambda getLambda(String alias) {
        return null;
    }
}
