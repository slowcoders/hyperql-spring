package org.slowcoders.hyperquery.impl;

import org.slowcoders.hyperquery.core.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class HSchema extends HModel {
    private final Class<? extends QRecord<?>> entityType;
    private final String tableName;
    private Map<String, QJoin> joins;
    private Map<String, QLambda> lambdas;
    private Map<String, QAttribute> attributes;

    private static final HashMap<Class<?>, HSchema> relations = new HashMap<>();

    public HSchema(Class<? extends QRecord<?>> entityType) {
        this.entityType = entityType;
        QFrom from = entityType.getAnnotation(QFrom.class);
        this.tableName = from.value();
    }
    public final Class<? extends QRecord<?>> getEntityType() { return entityType; }

    public QAttribute getAttribute(String property) {
        this.initialize();
        return attributes.get(property);
    }

    public QLambda getLambda(String alias) {
        return lambdas.get(alias);
    }
    public static HSchema registerSchema(Class<? extends QEntity<?>> clazz) {
        HSchema relation = relations.get(clazz);
        if (relation == null) {
            synchronized (relations) {
                relation = relations.get(clazz);
                if (relation == null) {
                    relation = new HSchema(clazz);
                    relations.put(clazz, relation);
                }
            }
        }
        return relation;
    }

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
                    joins.put("@" + f.getName(), join);
                }
                else if (QLambda.class.isAssignableFrom(propertyType)) {
                    f.setAccessible(true);
                    QLambda lambda = (QLambda) f.get(null);
                    lambda.init(this, f.getName());
                    lambdas.put(f.getName(), lambda);
                }
                else if (QAttribute.class.isAssignableFrom(propertyType)) {
                    f.setAccessible(true);
                    QAttribute attr = (QAttribute) f.get(null);
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

    static HSchema getSchema(Class<?> clazz) {
        HSchema schema = relations.get(clazz);
        if (schema != null) return schema;

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
                    if (QRecord.class.isAssignableFrom((Class<?>)params[0])) {
                        genericClass = (Class<?>) params[0];
                        break;
                    }
                }
            }
            if (genericClass == null) {
                throw new IllegalArgumentException(clazz.getName() + " is not valid a model(QEntity, QRecord or QFilter)");
            }
        }
        schema = HSchema.registerSchema((Class<? extends QEntity<?>>) genericClass);
        relations.put(genericClass, schema);
        return schema;
    }



    @Override
    protected HSchema loadSchema() {
        return this;
    }

    @Override
    protected String getQuery() {
        return "";
    }

    @Override
    protected String getTableName() {
        return this.tableName;
    }

    public QJoin getJoin(String join) {
        this.initialize();
        int next = join.indexOf('@', 1);
        String nextJoin = null;
        if (next > 0) {
            nextJoin = join.substring(next);
            join = join.substring(0, next);
            QJoin subJoin = getJoin(join);
            return subJoin.getTargetRelation().getJoin(nextJoin);
        }
        return joins.get(join);
    }
}
