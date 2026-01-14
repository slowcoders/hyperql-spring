package org.slowcoders.hyperquery.impl;

import org.slowcoders.hyperquery.core.QRecord;

public interface ViewResolver {
    String resolveView(Class<? extends QRecord<?>> entityType);
}
