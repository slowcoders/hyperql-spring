package org.slowcoders.hyperquery.impl;

import org.slowcoders.hyperquery.core.QEntity;
import org.slowcoders.hyperquery.core.QFrom;
import org.slowcoders.hyperquery.core.QJoin;
import org.slowcoders.hyperquery.core.QView;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class HSchema extends QView {
    private final Class<? extends QView> entityType;
    private Map<String, QJoin> joins;
    private Map<String, QLambda> lambdas;
    private Map<String, String> properties;

    private static final HashMap<Class<?>, HSchema> relations = new HashMap<>();

    public HSchema(Class<? extends QView> entityType) {
        super(getTableName(entityType));
        this.entityType = entityType;
    }

    static String getTableName(Class<? extends QView> entityType) {
        QFrom from = entityType.getAnnotation(QFrom.class);
        return from.value();
    }
    public final Class<? extends QView> getEntityType() { return entityType; }

    public String translateProperty(String property) {
        int p = property.lastIndexOf('.');
        if (p > 0) {
            String alias = property.substring(0, p);
            String name = property.substring(p + 1);
            String[] joinStack = alias.split("@");
            QView view = getView(this, joinStack);
            property = view.translateProperty(name);
        } else {
            property = properties.get(property);
        }
        return property;
    }

    private static QView getView(QView view, String[] joinStack) {
        for (String alias : joinStack) {
            if (alias.isEmpty()) continue;
            view.initialize();
            view = view.getJoin(alias).getTargetRelation();
        }
        return view;
    }

    public QLambda getLambda(String alias) {
        return lambdas.get(alias);
    }
    public QLambda getLambda(String[] joinPath, String property) {
        initialize();
        QView view = getView(this, joinPath);
        return view.getLambda(property);
    }

    public static HSchema getView(Class<? extends QView> clazz) { return relations.get(clazz); }

    public static HSchema registerSchema(Class<? extends QView> clazz) {
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
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        this.lambdas = lambdas;
        this.joins = joins;
    }

    public String getJoinTarget() {
        return super.getQuery();
    }

    public QJoin getJoin(String join) {
        this.initialize();
        return joins.get(join);
    }
}
