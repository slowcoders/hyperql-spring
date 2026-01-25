package org.slowcoders.hyperquery.core;

import org.slowcoders.hyperquery.impl.AliasNode;
import org.slowcoders.hyperquery.impl.HModel;
import org.slowcoders.hyperquery.impl.HSchema;

import java.sql.Connection;

public class QJoin extends AliasNode {

    public enum JoinType {
        Inner,
        InnerLateral,
        Left,
        LeftLateral,
        Right,
        Cross
    }
    private HModel model;
    private final boolean toUnique;
    private Class<? extends QEntity<?>> viewType;

    // Cross Join 은 지원하지 읺는다.
    protected QJoin(HModel inlineView, String joinOn, boolean toUnique) {
        super(joinOn);
        this.viewType = HiddenView.class;
        this.model = inlineView;
        this.toUnique = toUnique;
    }

    protected QJoin(Class<? extends QEntity<?>> viewType, String joinOn, boolean toUnique) {
        super(joinOn);
        this.viewType = viewType;
        this.toUnique = toUnique;
    }

    public String getJoinCriteria() {
        return super.getEncodedExpr();
    }
    public boolean isToUnique() { return toUnique; }

    public HModel getTargetRelation(Connection dbConn) {
        if (model == null) {
            model = HSchema.loadSchema(viewType, false, dbConn);
        }
        return model;
    }

    public static QJoin of(Class<? extends QEntity<?>> recordType, String joinOn) {
        return new QJoin(recordType, joinOn, false);
    }

    @QFrom("")
    private static class HiddenView implements QEntity<HiddenView> {}
}
