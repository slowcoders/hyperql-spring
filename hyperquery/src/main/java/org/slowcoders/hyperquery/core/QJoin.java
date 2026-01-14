package org.slowcoders.hyperquery.core;

import org.slowcoders.hyperquery.impl.AliasNode;
import org.slowcoders.hyperquery.impl.HModel;
import org.slowcoders.hyperquery.impl.HSchema;

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
    protected QJoin(QInlineView inlineView, String joinOn, boolean toUnique) {
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

    public HModel getTargetRelation() {
        if (model == null) {
            model = HSchema.registerSchema(viewType);
        }
        return model;
    }

    @QFrom("")
    private static class HiddenView implements QEntity<HiddenView> {}
}
