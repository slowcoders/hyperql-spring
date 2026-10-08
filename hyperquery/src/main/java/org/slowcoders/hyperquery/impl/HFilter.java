package org.slowcoders.hyperquery.impl;

public class HFilter {

    static final ThreadLocal<String> _sql = new ThreadLocal<>();
    static final ThreadLocal<Object> _session = new ThreadLocal<>();
    final String get__sql__() { return _sql.get(); }
    final Object get__session__() { return _session.get(); }
}
