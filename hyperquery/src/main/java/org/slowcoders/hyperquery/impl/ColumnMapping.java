package org.slowcoders.hyperquery.impl;

import org.slowcoders.hyperquery.core.QColumn;

import java.lang.reflect.Field;

class ColumnMapping {
    final String qualifiedColumnName;
    final String columnName;
    final String fieldName;
    QColumn columnConfig;

    public ColumnMapping(String qualifiedColumnName, String fieldName) {
        this.qualifiedColumnName = qualifiedColumnName;
        this.columnName = qualifiedColumnName.substring(qualifiedColumnName.lastIndexOf('.') + 1);
        this.fieldName = fieldName;
    }

    public ColumnMapping(String namespace, String columnName, String fieldName) {
        this.qualifiedColumnName = namespace + '.' + columnName;
        this.columnName = columnName;
        this.fieldName = fieldName;
    }

    public ColumnMapping(String columnExpr, String propertyPrefix, Field f) {
        this(columnExpr, propertyPrefix + f.getName());
        this.columnConfig = f.getAnnotation(QColumn.class);
    }
}
