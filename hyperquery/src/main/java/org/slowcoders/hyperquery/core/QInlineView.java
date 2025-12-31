package org.slowcoders.hyperquery.core;

import org.slowcoders.hyperquery.impl.HModel;
import org.slowcoders.hyperquery.impl.HSchema;

public class QInlineView extends HModel {

    private final String viewDefinition;
    private static final HSchema schema = HSchema.registerSchema(QView.class);

    public QInlineView(String query) {
        viewDefinition = query;
    }

    @Override
    protected HSchema loadSchema() {
        return schema;
    }

    @Override
    public String getQuery() {
        return viewDefinition;
    }

    @Override
    public String getTableName() {
        return "";
    }
}
