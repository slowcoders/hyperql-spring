package org.slowcoders.hyperquery.core;

import org.slowcoders.hyperquery.impl.HModel;
import org.slowcoders.hyperquery.impl.HSchema;
import org.slowcoders.hyperquery.impl.ViewResolver;

public class QInlineView extends HModel {

    private final String viewDefinition;
    private static final HSchema schema = HSchema.registerSchema(HiddenView.class);

    public QInlineView(String query) {
        viewDefinition = query;
    }

    @Override
    protected HSchema loadSchema() {
        return schema;
    }

    @Override
    protected String getQuery(ViewResolver viewResolver) {
        return viewDefinition;
    }

    @Override
    protected String getTableName() {
        return "";
    }

    @QFrom("")
    private static class HiddenView implements QEntity<HiddenView> {}
}
