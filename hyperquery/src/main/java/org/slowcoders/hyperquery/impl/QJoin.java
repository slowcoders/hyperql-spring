package org.slowcoders.hyperquery.impl;

import org.slowcoders.hyperquery.core.QEntity;

public class QJoin {
    private final String sql;
    private final HqRelation relation;
    private final boolean toUnique;

    // Cross Join 은 지원하지 읺는다.
    public QJoin(Class<? extends QEntity> targetEntity, String sql, boolean toUnique) {
        this.relation = HqRelation.registerRelation(targetEntity);
        this.sql = sql;
        this.toUnique = toUnique;
    }

    public String getJoinCriteria() {
        return sql;
    }
    public boolean isToUnique() { return toUnique; }

    public HqRelation getTargetRelation() {
        return relation;
    }
}
