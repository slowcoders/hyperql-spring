package org.slowcoders.hyperquery.core;

import org.slowcoders.hyperquery.impl.QJoin;
import org.slowcoders.hyperquery.impl.QLambda;

public class QView {

    private final String query;
    protected QView(String query) {
        this.query = query;
    }

    public static QView from(String query) {
        return new QView(query);
    }
    public final String getQuery() {
        return query;
    }

    public void initialize() {
    }

    public String translateProperty(String property) {
        return property;
    }

    public QJoin getJoin(String alias) {
        return null;
    }

    public QLambda getLambda(String alias) {
        return null;
    }
}
