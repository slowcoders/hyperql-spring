package org.slowcoders.hyperquery.impl;

import org.slowcoders.hyperquery.core.*;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class HModel {

    public void initialize(Connection conn) throws SQLException {
    }

    protected abstract HSchema loadSchema(JdbcConnector dbConn);

    protected abstract String getTableName();

    protected Object getTableExpression(ViewResolver viewResolver) { return ""; }

    protected QAttribute getAttribute(String property) {
        return null;
    }

    protected QJoin getJoin(String alias, JdbcConnector dbConn) {
        return null;
    }

    protected QLambda getLambda(String alias) {
        return null;
    }

    public String getColumnExpr(Field f) {
        return HSchema.Helper.getColumnName(f);
    }
}
