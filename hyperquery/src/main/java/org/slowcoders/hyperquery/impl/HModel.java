package org.slowcoders.hyperquery.impl;

import org.slowcoders.hyperquery.core.*;

public abstract class HModel {

    public void initialize() {
    }

    protected abstract HSchema loadSchema();

    protected abstract String getTableName();

    protected Object getTableExpression(ViewResolver viewResolver) { return ""; }

    protected QAttribute getAttribute(String property) {
        return null;
    }

    protected QJoin getJoin(String alias) {
        return null;
    }

    protected QLambda getLambda(String alias) {
        return null;
    }

}
