package org.slowcoders.hyperquery.core;

import java.util.EnumSet;

public class QTrigger {
    private final String sql;
    public QTrigger(String sql) {
        this.sql = sql;
    }

    public enum Event {
        INSERT, UPDATE, DELETE, TRUNCATE
    }
    public static QTrigger on(EnumSet<Event> events, Class<?> entity, String method) {
        return new QTrigger("ON " + events);
    }
}
