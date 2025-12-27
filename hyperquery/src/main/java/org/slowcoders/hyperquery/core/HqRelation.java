package org.slowcoders.hyperquery.core;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class HqRelation {
    private Class<?> clazz;
    private Map<String, QJoin> joins;
    private Map<String, QLambda> lambdas;
    private Map<String, String> properties;

    private static final HashMap<Class<?>, HqRelation> relations = new HashMap<>();

    public HqRelation(Class<?> clazz) {
        this.clazz = clazz;
    }

    public String resolveProperty(String property) {
        int p = property.lastIndexOf('.');
        String view = property.substring(0, p);
        String name = property.substring(p+1);
        String[] joinStack = view.split("@");
        HqRelation relation = getRelation(this, joinStack);
        String resolved = relation.properties.get(name);
        return resolved != null ? resolved : property;
    }

    private static HqRelation getRelation(HqRelation hqRelation, String[] joinStack) {
        for (String alias : joinStack) {
            if (alias.isEmpty()) continue;
            hqRelation.initialize();
            hqRelation = hqRelation.joins.get(alias).getTargetRelation();
        }
        return hqRelation;
    }

    public QLambda getLambda(String[] joinPath, String property) {
        initialize();
        HqRelation relation = getRelation(this, joinPath);
        return relation.lambdas.get(property);
    }

    public static HqRelation getRelation(Class<?> clazz) { return relations.get(clazz); }

    public static HqRelation registerRelation(Class<?> clazz) {
        HqRelation relation = relations.get(clazz);
        if (relation == null) {
            synchronized (relations) {
                relation = relations.get(clazz);
                if (relation == null) {
                    relation = new HqRelation(clazz);
                    relations.put(clazz, relation);
                }
            }
        }
        return relation;
    }

    private synchronized void initialize() {
        if (joins != null) return;

        HashMap<String, QJoin> joins = new HashMap<>();
        HashMap<String, QLambda> lambdas = new HashMap<>();
        try {
            for (Field f : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                Class<?> propertyType = f.getType();
                if (propertyType == QJoin.class) {
                    f.setAccessible(true);
                    QJoin join = (QJoin) f.get(null);
                    joins.put(f.getName(), join);
                }
                else if (propertyType == QLambda.class) {
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

}
