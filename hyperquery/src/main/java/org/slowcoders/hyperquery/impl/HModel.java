package org.slowcoders.hyperquery.impl;

import org.slowcoders.hyperquery.core.*;

import java.sql.Connection;

public abstract class HModel {

    public void initialize() {
    }

    protected abstract HSchema loadSchema(Connection dbConn);

    protected abstract String getTableName();

    protected Object getTableExpression(ViewResolver viewResolver) { return ""; }

    protected QAttribute getAttribute(String property) {
        return null;
    }

    protected QJoin getJoin(String alias, Connection dbConn) {
        return null;
    }

    protected QLambda getLambda(String alias) {
        return null;
    }

}
