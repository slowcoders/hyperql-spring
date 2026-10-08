package org.slowcoders.hyperquery.util;

import java.util.LinkedHashMap;

public class KVEntity extends LinkedHashMap<String, Object> {

    public static KVEntity of(String key, Object value) {
        KVEntity entity = new KVEntity();
        entity.put(key, value);
        return entity;
    }

    public KVEntity add(String key, Object value) throws IllegalArgumentException {
        Object old_v = super.put(key, value);
        if (old_v != null) {
            throw new IllegalArgumentException("Duplicate key: " + key);
        }
        return this;
    }

    public String getString(String key) {
        return (String)super.get(key);
    }

    public Long getLong(String key) {
        return (Long)super.get(key);
    }

    public Integer getInt(String key) {
        return (Integer) super.get(key);
    }
}
