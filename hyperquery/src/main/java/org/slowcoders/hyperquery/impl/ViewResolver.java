package org.slowcoders.hyperquery.impl;

import org.slowcoders.hyperquery.core.QJoin;
import org.slowcoders.hyperquery.core.QRecord;

import java.util.Map;

public interface ViewResolver {
    Object resolveView(String namespace, String sqlFragmentId, Map<String, String> properties);

    HSchema loadSchema(Class<?> recordType, boolean isEntity);

    HSchema getTargetSchema(QJoin join);

    QJoin getJoin(HModel model, String alias);
}
