package org.slowcoders.hyperquery.core;

public class QAttribute {

    private final String sql;
    private QAttribute(String sql) {
        this.sql = sql;
    }

    public static QAttribute of(String statement) {
        return new QAttribute(statement);
    }

}
