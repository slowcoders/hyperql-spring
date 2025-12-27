package org.slowcoders.hyperquery.core;

public class QTable extends QView {
    protected QTable(Class<?> relation, String tableName) {
        super(relation, tableName);
    }

    public static QTable of(Class<?> relation, String tableName) {
        return new QTable(relation, tableName);
    }

}
