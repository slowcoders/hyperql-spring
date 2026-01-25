package org.slowcoders.hyperquery.core;

import org.slowcoders.hyperquery.impl.HModel;
import org.slowcoders.hyperquery.impl.HSchema;
import org.slowcoders.hyperquery.impl.ViewResolver;

import java.sql.Connection;

public class QInlineView extends HModel {

    private final String viewDefinition;

    public QInlineView(String query) {
        viewDefinition = query;
    }

    @Override
    protected HSchema loadSchema(Connection dbConn) {
        return HSchema.loadSchema((Class)QEntity.class, false, dbConn);
    }

    @Override
    protected String getTableExpression(ViewResolver viewResolver) {
        return viewDefinition;
    }

    @Override
    protected String getTableName() {
        return "";
    }

}
