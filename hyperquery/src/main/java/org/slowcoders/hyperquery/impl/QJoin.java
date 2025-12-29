package org.slowcoders.hyperquery.impl;

import org.slowcoders.hyperquery.core.QEntity;
import org.slowcoders.hyperquery.core.QView;

public class QJoin {
    private final String sql;
    private final QView view;
    private final boolean toUnique;

    // Cross Join 은 지원하지 읺는다.
    protected QJoin(QView view, String sql, boolean toUnique) {
        this.view = (QView) view;
        this.sql = sql;
        this.toUnique = toUnique;
    }

    public String getJoinCriteria() {
        return sql;
    }
    public boolean isToUnique() { return toUnique; }

    public QView getTargetRelation() {
        return view;
    }
}
