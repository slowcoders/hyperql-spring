package org.slowcoders.hyperquery.impl;

import jakarta.persistence.Column;
import org.slowcoders.hyperquery.core.QColumn;
import org.slowcoders.hyperquery.core.QEntity;
import org.slowcoders.hyperquery.core.QJoin;
import org.slowcoders.hyperquery.core.QRecord;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

public abstract class HModel {

    public void initialize() {
    }

    protected abstract HSchema loadSchema();

    protected abstract String getQuery();

    protected abstract String getTableName();

    protected QAttribute getAttribute(String property) {
        return null;
    }

    protected QJoin getJoin(String alias) {
        return null;
    }

    protected QLambda getLambda(String alias) {
        return null;
    }

    static Class<? extends QRecord<?>> getElementType(Field f) {
        if (f.getType().isArray()) return (Class<? extends QRecord<?>>) f.getType().getComponentType();

        Type type = f.getGenericType();
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] typeArguments = pType.getActualTypeArguments();
            if (typeArguments.length > 0 && typeArguments[0] instanceof Class) {
                return (Class<? extends QRecord<?>>) typeArguments[0];
            }
        }
        return (Class<? extends QRecord<?>>) f.getType();
    }

    static class Helper implements QEntity<Helper> {
        static String getColumnName(Field f) {
            QColumn anno = f.getAnnotation(QColumn.class);
            if (anno != null) return anno.value();

            PKColumn pk = f.getAnnotation(PKColumn.class);
            if (pk != null) return pk.value();

            TColumn tcol = f.getAnnotation(TColumn.class);
            if (tcol != null) return tcol.value();

            Column col = f.getAnnotation(Column.class);
            if (col != null) return col.name();

            return null;
        }

        public static boolean isUniqueKey(Field f) {
            return f.getAnnotation(PKColumn.class) != null;
        }

        public static boolean isCollectionType(Field f) {
            return f.getType().isArray() || Collection.class.isAssignableFrom(f.getType());
        }
    }
}
