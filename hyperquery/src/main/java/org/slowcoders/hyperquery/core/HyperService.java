package org.slowcoders.hyperquery.core;

import java.util.List;

public class HyperService<E extends QEntity<E>> {
    public E getEntity(Object id) {
        return null;
    }

    public List<E> findEntities(QFilter<E> filter) {
        return null;
    }

    public E insert(E entity) {
        return null;
    }

    public int insertOrUpdate(E entity) {
        return 0;
    }

    public int insert(List<E> entities) {
        return 0;
    }

    public int insertOrUpdate(List<E> entities) {
        return 0;
    }

    public int delete(Object id) {
        return 0;
    }

    public int delete(List<Object> idLit) {
        return 0;
    }
}
