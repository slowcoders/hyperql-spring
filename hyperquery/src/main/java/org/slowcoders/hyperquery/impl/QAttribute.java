package org.slowcoders.hyperquery.impl;

public class QAttribute {

    private final String sql;
    public QAttribute(String sql) {
        this.sql = sql;
    }

    public static QAttribute of(String statement) {
        return new QAttribute(statement);
    }

}
