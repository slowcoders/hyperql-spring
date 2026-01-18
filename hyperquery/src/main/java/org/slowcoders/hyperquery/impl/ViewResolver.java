package org.slowcoders.hyperquery.impl;

import java.util.Map;

public interface ViewResolver {
    Object resolveView(String namespace, String sqlFragmentId, Map<String, String> properties);
}
