package org.slowcoders.hyperquery.core;

public class QAggregation<T extends QRelation> extends QRecord<T> {
    public QAggregation(Class<T> relation, String[] groupBy) {
        super(relation);
    }
}
