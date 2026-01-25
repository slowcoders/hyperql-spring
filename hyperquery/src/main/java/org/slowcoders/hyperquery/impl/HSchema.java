package org.slowcoders.hyperquery.impl;

import jakarta.persistence.Column;
import org.slowcoders.hyperquery.core.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class HSchema extends HModel {
    private final Class<? extends QRecord<?>> entityType;
    private final String tableName;
    private Map<String, QJoin> joins;
    private Map<String, QLambda> lambdas;
    private Map<String, QAttribute> attributes;

    private static final HashMap<Class<?>, HSchema> relations = new HashMap<>();

    private static class EmptyEntity implements QEntity<EmptyEntity> {}
    static {
        relations.put(QEntity.class, new HSchema(EmptyEntity.class, false));
    }

    protected HSchema(Class<? extends QRecord<?>> entityType, boolean isEntity) {
        this.entityType = entityType;
        QFrom from = entityType.getAnnotation(QFrom.class);
        if (from != null) {
            this.tableName = from.value();
        } else if (isEntity) {
            throw new RuntimeException("No @QFrom or @QFromMapper annotation found for " + entityType.getName());
        } else {
            this.tableName = "";
        }
    }


    public final Class<? extends QRecord<?>> getEntityType() { return entityType; }

    public QAttribute getAttribute(String property) {
        this.initialize();
        return attributes.get(property);
    }

    public QLambda getLambda(String alias) {
        return lambdas.get(alias);
    }
//    public static HSchema loadSchema(Class<? extends QRecord<?>> clazz, boolean isEntity) {
//        HSchema relation = relations.get(clazz);
//        if (relation == null) {
//            synchronized (relations) {
//                relation = relations.get(clazz);
//                if (relation == null) {
//                    relation = new HSchema(clazz, isEntity);
//                    relations.put(clazz, relation);
//                }
//            }
//        }
//        return relation;
//    }

    public synchronized void initialize() {
        if (joins != null) return;

        HashMap<String, QJoin> joins = new HashMap<>();
        HashMap<String, QLambda> lambdas = new HashMap<>();
        HashMap<String, QAttribute> attributes = new HashMap<>();
        try {
            for (Field f : entityType.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                Class<?> propertyType = f.getType();
                if (QJoin.class.isAssignableFrom(propertyType)) {
                    f.setAccessible(true);
                    QJoin join = (QJoin) f.get(null);
                    ((AliasNode)join).setName(f.getName());
                    joins.put("@" + f.getName(), join);
                }
                else if (QLambda.class.isAssignableFrom(propertyType)) {
                    f.setAccessible(true);
                    QLambda lambda = (QLambda) f.get(null);
                    ((AliasNode)lambda).setName(f.getName());
                    lambdas.put(f.getName(), lambda);
                }
                else if (QAttribute.class.isAssignableFrom(propertyType)) {
                    f.setAccessible(true);
                    QAttribute attr = (QAttribute) f.get(null);
                    ((AliasNode)attr).setName(f.getName());
                    attributes.put(f.getName(), attr);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        this.lambdas = lambdas;
        this.joins = joins;
        this.attributes = attributes;
    }

    public static HSchema loadSchema(Class<?> clazz, boolean isEntity, Connection dbConn) {
        synchronized (relations) {
            HSchema schema = relations.get(clazz);
            if (schema != null) return schema;
        }

        Class<?> genericClass = clazz;
        while (!QEntity.class.isAssignableFrom(genericClass)) {
            if (QFilter.class.isAssignableFrom(genericClass)) {
                Type type = genericClass.getGenericSuperclass();
                if (type instanceof ParameterizedType) {
                    genericClass = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
                } else {
                    type = type.getClass();
                }
                continue;
            }

            Type[] interfaces = genericClass.getGenericInterfaces();
            genericClass = null;
            for (Type iface : interfaces) {
                if (iface instanceof ParameterizedType) {
                    Type[] params = ((ParameterizedType) iface).getActualTypeArguments();
                    if (QRecord.class.isAssignableFrom((Class<?>) params[0])) {
                        genericClass = (Class<?>) params[0];
                        break;
                    }
                }
            }
            if (genericClass == null) {
                throw new IllegalArgumentException(clazz.getName() + " is not valid a model(QEntity, QRecord or QFilter)");
            }
        }

        synchronized (relations) {
            HSchema schema = relations.get(genericClass);
            if (schema == null) {
                schema = new HSchema((Class<? extends QEntity<?>>) genericClass, isEntity);
                relations.put(genericClass, schema);
            }
            return schema;
        }
    }



    @Override
    protected HSchema loadSchema(Connection dbConn) {
        return this;
    }

    @Override
    protected String getTableName() {
        return this.tableName;
    }

    public QJoin getJoin(String join, Connection dbConn) {
        this.initialize();
        int next = join.indexOf('@', 1);
        String nextJoin = null;
        if (next > 0) {
            nextJoin = join.substring(next);
            join = join.substring(0, next);
            QJoin subJoin = getJoin(join, dbConn);
            return subJoin.getTargetRelation(dbConn).getJoin(nextJoin, dbConn);
        }
        return joins.get(join);
    }

    String getColumnExpr(Field f) {
        String columnExpr = Helper.getColumnName(f);
        if (columnExpr != null) return columnExpr;
        if (this.attributes != null) {
            QAttribute attr = this.attributes.get(f.getName());
            if (attr != null) return attr.getEncodedExpr();
        }
        return null;
    }

    public static class TablePath {
        private final String catalog;
        private final String schema;
        private final String simpleName;

        TablePath(String catalog, String schema, String simpleName) {
            this.catalog = (catalog);
            this.schema = (schema);
            this.simpleName = (simpleName);
        }


        public String getSimpleName() {
            return simpleName;
        }

        public String getCatalog() {
            return catalog;
        }

        public String getSchema() {
            return schema;
        }
    }
    private ArrayList<String> getPrimaryKeys(Connection conn, TablePath tablePath) throws SQLException {
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getPrimaryKeys(tablePath.getCatalog(), tablePath.getSchema(), tablePath.getSimpleName());
        ArrayList<String> keys = new ArrayList<>();
        int next_key_seq = 1;
        while (rs.next()) {
            String key = rs.getString("column_name");
            if (false) {
                // postgresql 에서만 동작.
                int seq = rs.getInt("key_seq");
                if (seq != next_key_seq) {
                    throw new RuntimeException("something wrong");
                }
                next_key_seq++;
            }
            keys.add(key);
        }
        return keys;
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
    }
}
