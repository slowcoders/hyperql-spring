package org.slowcoders.hyperquery.core;

public class QRecord<T> {
    private final Class<T> relation;

    public QRecord(Class<T> relation) {
        this.relation = relation;
    }

    public final Class<T> getRelation() {
        return relation;
    }
}
