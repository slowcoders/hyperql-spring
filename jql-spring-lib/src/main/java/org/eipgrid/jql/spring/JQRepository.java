package org.eipgrid.jql.spring;

import org.eipgrid.jql.JQSelect;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface JQRepository<ENTITY, ID> {

    Class<ENTITY> getEntityType();

    ID convertId(Object v);

    List<ENTITY> find(Map<String, Object> jsQuery, JQSelect columns);

    ENTITY findTop(Map<String, Object> jsQuery, Sort sort);

    ENTITY find(ID id);

    default ENTITY get(ID id) throws IllegalArgumentException {
        ENTITY entity = find(id);
        if (entity == null) throw new IllegalArgumentException("entity not found: " + id);
        return entity;
    }

    List<ENTITY> list(Collection<ID> idList);

    default Iterable<ENTITY> listAll() {
        return find(null, JQSelect.Whole);
    }



    long count(Map<String, Object> jsQuery);



    ID insert(ENTITY entity);

    ID insert(Map<String, Object> dataSet) throws IOException;

    List<ID> insertAll(Collection<Map<String, Object>> dataSet) throws IOException;


    void update(ID id, Map<String, Object> updateSet) throws IOException;

    void update(Collection<ID> idList, Map<String, Object> updateSet) throws IOException;

//    void update(Map<String, Object> filter, Map<String, Object> updateSet) throws IOException;

    void delete(ID id);

    int delete(Collection<ID> idList);

//    void delete(Map<String, Object> filter) throws IOException;

    void clearEntityCache(ID id);

}
