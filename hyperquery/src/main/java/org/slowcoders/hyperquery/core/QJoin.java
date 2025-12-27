package org.slowcoders.hyperquery.core;

public class QJoin {
    private final String sql;
    private final HqRelation relation;
    private final boolean toUnique;

    // Cross Join 은 지원하지 읺는다.
    public QJoin(Class<?> relation, String sql, boolean toUnique) {
        this.relation = HqRelation.registerRelation(relation);
        this.sql = sql;
        this.toUnique = toUnique;
    }

    public String getJoinCriteria() {
        return sql;
    }
    public boolean isToUnique() { return toUnique; }
    public static QJoin toSingle(Class<?> entity, String joinOn) {
        return new QJoin(entity, joinOn, true);
    }

    public static QJoin toSingle(QView view, String joinOn) {
        return new QJoin(null, joinOn, true);
    }

    public static QJoin toMulti(Class<?> entity, String joinOn) {
        return new QJoin(entity, joinOn, false);
    }

    public static QJoin toMulti(QView view, String joinOn) {
        return new QJoin(null, joinOn, false);
    }

    public HqRelation getTargetRelation() {
        return relation;
    }
}
