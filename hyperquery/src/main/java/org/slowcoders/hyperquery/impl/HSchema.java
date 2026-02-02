package org.slowcoders.hyperquery.impl;

import jakarta.persistence.Column;
import org.slowcoders.hyperquery.core.*;
import org.slowcoders.hyperquery.impl.jdbc.JdbcColumn;
import org.slowcoders.hyperquery.impl.jdbc.JdbcSchemaLoader;
import org.slowcoders.hyperquery.impl.jdbc.PGSchemaLoader;
import org.springframework.data.annotation.Transient;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class HSchema extends HModel {
    private final Class<? extends QRecord<?>> entityType;
    private final String tableName;
    private Map<String, QJoin> joins;
    private Map<String, QLambda> lambdas;
    private Map<String, QAttribute> attributes;

    private static final HashMap<Class<?>, HSchema> relations = new HashMap<>();
    private ArrayList<JdbcColumn> jdbcColumns;
    private ArrayList<String> pkColumnNames;

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

    public final List<String> getPrimaryKeys() {
        return this.pkColumnNames;
    }

    public QAttribute getAttribute(String property) {
//        this.initialize(conn);
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

    static JdbcSchemaLoader jdbcSchemaLoader = null;
    public synchronized void initialize(Connection conn) throws SQLException {
        if (joins != null) return;

        if (jdbcSchemaLoader == null) {
            jdbcSchemaLoader = new PGSchemaLoader(conn);
        }
        if (!this.tableName.isEmpty()) {
            JdbcSchemaLoader.TablePath tablePath = jdbcSchemaLoader.makeTablePath(tableName);
            this.pkColumnNames = jdbcSchemaLoader.getPrimaryKeys(conn, tablePath);
            this.jdbcColumns = jdbcSchemaLoader.getColumns(conn, new HashMap<>(), tablePath, pkColumnNames);
        }

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
                    ((AliasNode)join).setName(this, f.getName());
                    joins.put("@" + f.getName(), join);
                }
                else if (QLambda.class.isAssignableFrom(propertyType)) {
                    f.setAccessible(true);
                    QLambda lambda = (QLambda) f.get(null);
                    ((AliasNode)lambda).setName(this, f.getName());
                    lambdas.put(f.getName(), lambda);
                }
                else if (QAttribute.class.isAssignableFrom(propertyType)) {
                    f.setAccessible(true);
                    QAttribute attr = (QAttribute) f.get(null);
                    ((AliasNode)attr).setName(this, f.getName());
                    attributes.put(f.getName(), attr);
                }
                else {

                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        this.lambdas = lambdas;
        this.joins = joins;
        this.attributes = attributes;
    }

    public static Class<QEntity<?>> getRootEntityType(Class<?> clazz) {
        boolean isRecord = QRecord.class.isAssignableFrom(clazz);
        while (true) {
            Class<?> _super = clazz.getSuperclass();
            if (isRecord) {
                if (!QRecord.class.isAssignableFrom(_super))
                    break;
            }
            else if (_super == QStore.class ||
                     _super == QFilter.class) {
                break;
            }
            clazz = _super;
        }

        while (!QEntity.class.isAssignableFrom(clazz)) {
            Type type = clazz.getGenericSuperclass();
            if (type instanceof ParameterizedType) {
                clazz = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
                continue;
            }

            Type[] interfaces = clazz.getGenericInterfaces();
            for (Type iface : interfaces) {
                if (iface instanceof ParameterizedType) {
                    Type[] params = ((ParameterizedType) iface).getActualTypeArguments();
                    if (QRecord.class.isAssignableFrom((Class<?>) params[0])) {
                        return (Class<QEntity<?>>) params[0];
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return (Class<QEntity<?>>) clazz;
    }
    public static HSchema loadSchema(Class<?> clazz, boolean isEntity, JdbcConnector dbConn) {
        synchronized (relations) {
            HSchema schema = relations.get(clazz);
            if (schema != null) return schema;
        }

        Class<?> genericClass = getRootEntityType(clazz);
        if (genericClass == null) {
            throw new IllegalArgumentException(clazz.getName() + " is not valid a model(QEntity, QRecord or QFilter)");
        }

        synchronized (relations) {
            HSchema schema = relations.get(genericClass);
            if (schema == null) {
                final HSchema newSchema = new HSchema((Class<? extends QEntity<?>>) genericClass, isEntity);
                dbConn.execute(conn -> {
                    newSchema.initialize(conn);
                    return null;
                });
                schema = newSchema;
                relations.put(genericClass, schema);
            }
            return schema;
        }
    }



    @Override
    protected HSchema loadSchema(JdbcConnector dbConn) {
        return this;
    }

    @Override
    protected String getTableName() {
        return this.tableName;
    }

    public QJoin getJoin(String join, JdbcConnector dbConn) {
//        this.initialize(conn);
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

    public String getColumnType(String columnName) {
        for (JdbcColumn col : jdbcColumns) {
            if (columnName.equals(col.getPhysicalName())) {
                return col.getColumnTypeName();
            }
        }
        throw new RuntimeException("column not found: " + columnName);
    }

    public String getColumnExpr(Field f) {
        String columnExpr = Helper.getColumnName(f);
        if (columnExpr != null) return columnExpr;
        String name = f.getName();
        if (this.attributes != null) {
            QAttribute attr = this.attributes.get(name);
            if (attr != null) return attr.getEncodedExpr();
        }
        for (JdbcColumn col : jdbcColumns) {
            if (name.equals(col.getFieldName())) {
                return col.getPhysicalName();
            }
        }
        QJoin join = this.joins.get("@" + name);
        if (join != null) {
            return "@" + name;
        }
        return null;
    }

    static class Helper implements QEntity<Helper> {
        static String getColumnName(Field f) {
            QColumn anno = f.getAnnotation(QColumn.class);
            if (anno != null) return anno.name();

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

        public static boolean isTransient(Field f) {
            return Modifier.isTransient(f.getModifiers())
                    || f.getAnnotation(Transient.class) != null;
        }
    }
}
