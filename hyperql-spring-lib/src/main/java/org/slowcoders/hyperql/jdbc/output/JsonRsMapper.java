package org.slowcoders.hyperql.jdbc.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slowcoders.hyperql.RestTemplate;
import org.slowcoders.hyperql.js.JsUtil;
import org.slowcoders.hyperql.schema.QColumn;
import org.slowcoders.hyperql.schema.QResultMapping;
import org.slowcoders.hyperql.schema.QSchema;
import org.slowcoders.hyperql.util.KVEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.JdbcUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class JsonRsMapper implements JdbcResultMapper<KVEntity> {
    private final List<QResultMapping> resultMappings;
    private final ObjectMapper objectMapper;
    private ResultCache resultCacheRoot;
    private CachedEntity baseEntity = null;
    private ArrayList<KVEntity> results = new ArrayList<>();
    private MappedColumn[] mappedColumns;

    public JsonRsMapper(List<QResultMapping> rowMappings, ObjectMapper objectMapper) {
        this.resultMappings = rowMappings;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<KVEntity> extractData(ResultSet rs) throws SQLException, DataAccessException {
        this.mappedColumns = initMappedColumns(rs);
        this.resultCacheRoot = new ResultCache(0, resultMappings.size());

        final int cntMapping = resultMappings.size();
        while (rs.next()) {
            int idxColumn = 0;
            int cntColumn;
            QResultMapping lastMapping = null;
            ResultCache resultCache = resultCacheRoot;
            read_mapping:
            for (int idxMapping = 0; idxMapping < cntMapping; idxMapping++, idxColumn+=cntColumn) {
                QResultMapping mapping = resultMappings.get(idxMapping);
                if (mapping.getParentNode() != lastMapping) {
                    resultCache = resultCacheRoot;
                }
                lastMapping = mapping;

                List<QColumn> columns = mapping.getSelectedColumns();
                cntColumn = columns.size();
                if (cntColumn == 0) continue;

                boolean isArray = mapping.isArrayNode();// && mapping.hasArrayDescendantNode();
                if (!isArray || !mapping.hasJoinedChildMapping()) {
                    if (idxColumn + cntColumn < this.mappedColumns.length) {
                        // TODO check Error!!
                        // SimpleForm:book_order, author.id 와 book.id 를 모두 select하면 오류 발생.
                        // 2 개 이상의 subArray Table 을 Join 검색하면서 발생하는 문제.. (resultSet 길이가 각 Join 결과 갯수를 모두 곱한 값을 가지게 된다.)
                        // throw new RuntimeException("multi sub array join is not allowed!");
                    }
                    readColumns(rs, idxColumn, cntColumn);
                    continue;
                }

                List<QColumn> pkColumns = mapping.getSchema().getPKColumns();
                int pkIndex = idxColumn;
                for (int pkCount = pkColumns.size(); --pkCount >= 0; pkIndex++) {
                    Object value = getColumnValue(rs, pkIndex + 1, mappedColumns[pkIndex].column);
                    mappedColumns[pkIndex].value = value;
                    if (value == null) {
                        if (isArray && pkIndex > 0) {
                            makeSubArray(mapping);
                        }
                        continue read_mapping;
                    }
                }

                Object key = makeCacheKey(mapping.getSchema(), idxColumn);
                if (key == null) {
                    readColumns(rs, idxColumn, cntColumn);
                    continue;
                }

                resultCache = resultCache.getCache(idxMapping);
                CachedEntity cachedEntity = resultCache.get(key);
                if (cachedEntity == null) {
                    cachedEntity = readColumns(rs, idxColumn, cntColumn);
                    resultCache.put(key, cachedEntity);
                } else if (idxMapping > 0) {
                    CachedEntity entity = makeBaseEntity(mapping);
                    if (cachedEntity.addParent(entity)) {
                        String[] entityPath = mapping.getEntityMappingPath();
                        String key2 = entityPath[entityPath.length - 1];
                        List data = (List) entity.get(key2);
                        if (data == null) {
                            data = new ArrayList();
                            entity.put(key2, data);
                        }
                        data.add(cachedEntity);
                    }
                }
                if (idxColumn == 0) {
                    baseEntity = cachedEntity;
                }
            }
        }
        return results;
    }

    private void makeSubArray(QResultMapping mapping) {
        JdbcEntity base = makeBaseEntity(mapping);
        String[] entityPath = mapping.getEntityMappingPath();
        String key = entityPath[entityPath.length - 1];
        if (base.get(key) == null) {
            base.put(key, new ArrayList());
        }
    }

    private Object makeCacheKey(QSchema schema, int idxColumn) {
        List<QColumn> pkColumns = schema.getPKColumns();
        int cntPk = pkColumns.size();
        if (cntPk == 0) {
            pkColumns = schema.getReadableColumns();
            cntPk = pkColumns.size();
        }
        if (cntPk == 1) {
            return mappedColumns[idxColumn].value;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cntPk; i ++) {
            sb.append(mappedColumns[idxColumn++].value);
            sb.append("+");
        }
        return sb.toString();
    }

    private static void putValue(CachedEntity entity, MappedColumn mappedColumn, Object value) {
        CachedEntity node = entity;
        QColumn column = mappedColumn.column;
        String fieldName = column.getJsonKey(); /* .getJavaFieldName();
        for (JQColumn pk; (pk = column.getJoinedPrimaryColumn()) != null; ) {
            CachedEntity pkEntity = (CachedEntity) node.get(fieldName);
            if (pkEntity == null) {
                pkEntity = new CachedEntity(node);
                node.put(fieldName, pkEntity);
            }
            node = pkEntity;
            column = pk;
            fieldName = column.getJavaFieldName();
        }
        */
        Object old = node.put(fieldName, value);
        if (old != null && !old.equals(value)) {
            throw new RuntimeException("something wrong");
        }
    }

    private CachedEntity readColumns(ResultSet rs, int idxColumn, int count) throws SQLException, DataAccessException {
        int end = idxColumn + count;
        CachedEntity currEntity;
        if (idxColumn == 0) {
            baseEntity = new CachedEntity(null);
            results.add(baseEntity);
        }

        currEntity = makeSubEntity(mappedColumns[idxColumn].mapping);

        for (; idxColumn < end; ) {
            MappedColumn mappedColumn = mappedColumns[idxColumn++];
            Object value = getColumnValue(rs, idxColumn, mappedColumn.column);
            mappedColumn.value = value;
            putValue(currEntity, mappedColumn, value);
        }
        return currEntity;
    }

    private CachedEntity makeBaseEntity(QResultMapping currMapping) {
        CachedEntity currEntity = baseEntity;
        String[] entityPath = currMapping.getEntityMappingPath();
        int idxLastPath = entityPath.length - 1;
        for (int i = 0; i < idxLastPath; i++) {
            currEntity = makeSubEntity(currEntity, entityPath[i], false);
        }
        return currEntity;
    }

    private CachedEntity makeSubEntity(QResultMapping currMapping) {
        CachedEntity currEntity = makeBaseEntity(currMapping);
        String[] entityPath = currMapping.getEntityMappingPath();
        int last = entityPath.length - 1;
        if (last >= 0) {
            currEntity = makeSubEntity(currEntity, entityPath[last], currMapping.isArrayNode());
        }
        return currEntity;
    }

    private CachedEntity makeSubEntity(CachedEntity entity, String key, boolean isArray) {
        Object subEntity = entity.get(key);
        if (subEntity == null) {
            subEntity = new CachedEntity(entity);
            if (isArray) {
                ArrayList<Object> array = new ArrayList<>();
                array.add(subEntity);
                entity.put(key, array);
            } else {
                entity.put(key, subEntity);
            }
        } else if (isArray) {
            if (subEntity instanceof CachedEntity) {
                /** TODO remove this tricky code */
                ArrayList<Object> array = new ArrayList<>();
                entity.put(key, array);
                array.add(subEntity);
            } else {
                ArrayList<Object> array = (ArrayList<Object>) subEntity;
                subEntity = new CachedEntity(entity);
                array.add(subEntity);
            }
        } else if (subEntity instanceof ArrayList) {
            ArrayList<CachedEntity> list = (ArrayList<CachedEntity>) subEntity;
            if (list.isEmpty()) {
                list.add(new CachedEntity(entity));
            }
            subEntity = list.get(list.size()-1);
        }
        return (CachedEntity)subEntity;
    }

    protected Object getColumnValue(ResultSet rs, int index, QColumn column) throws SQLException {
        Object value;
        if (column.isJsonNode()) {
            value = JsUtil.parseJson(objectMapper, rs.getString(index));
        } else {
            value = JdbcUtils.getResultSetValue(rs, index);
        }
        return value;
    }


    private MappedColumn[] initMappedColumns(ResultSet rs) throws SQLException {

        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();

        MappedColumn[] mappedColumns = new MappedColumn[columnCount];
        ColumnMappingHelper helper = new ColumnMappingHelper();

        int idxColumn = 0;
        for (QResultMapping mapping : resultMappings) {
            List<QColumn> columns = mapping.getSelectedColumns();
            if (columns.size() == 0) {
                continue;
            }
            helper.reset(mapping.getEntityMappingPath());
            for (QColumn column : columns) {
                String[] path = helper.getEntityMappingPath(column);
                mappedColumns[idxColumn++] = new MappedColumn(mapping, column, path);
            }
        }
        if (idxColumn != columnCount) {
            throw new RuntimeException("Something wrong!");
        }
        return mappedColumns;
    }

    @Override
    public void setOutputMetadata(RestTemplate.Response response) {
        return;
    }


    private static class ColumnMappingHelper extends HashMap<String, ColumnMappingHelper> {
        String[] entityPath;

        void reset(String[] jsonPath) {
            this.entityPath = jsonPath;
            this.clear();
        }

        public String[] getEntityMappingPath(QColumn column) {
            String jsonKey = column.getJsonKey();
            ColumnMappingHelper cache = this;
            for (int p; (p = jsonKey.indexOf('.')) > 0; ) {
                cache = cache.register(entityPath, jsonKey.substring(0, p));
                jsonKey = jsonKey.substring(p + 1);
            }
            return cache.entityPath;

        }

        public ColumnMappingHelper register(String[] basePath, String key) {
            ColumnMappingHelper cache = this.get(key);
            if (cache == null) {
                cache = new ColumnMappingHelper();
                cache.entityPath = toJsonPath(basePath, key);
            }
            return cache;
        }

        private String[] toJsonPath(String[] basePath, String key) {
            String[] jsonPath = new String[basePath.length + 1];
            System.arraycopy(basePath, 0, jsonPath, 0, basePath.length);
            jsonPath[basePath.length] = key;
            return jsonPath;
        }
    }

    private static class MappedColumn {
        final QColumn column;
        final QResultMapping mapping;
        final String[] mappingPath;
        private Object   value;

        public MappedColumn(QResultMapping mapping, QColumn column, String[] path) {
            this.mapping = mapping;
            this.column = column;
            this.mappingPath = path;
        }
    }

    private static class ResultCache extends HashMap<Object, CachedEntity> {
        private final int id;
        private final ResultCache[] caches;

        ResultCache(int id, int cntSlot) {
            this.id = id;
            caches = new ResultCache[cntSlot];
        }

        ResultCache getCache(int idxSlot) {
            if (idxSlot == this.id) return this;

            ResultCache cache = caches[idxSlot];
            if (cache == null) {
                caches[idxSlot] = cache = new ResultCache(idxSlot, caches.length);
            }
            return cache;
        }
    }

    private static class CachedEntity extends JdbcEntity {
        private static int g_sno;
        private final int id;

        private final transient HashSet<CachedEntity> parents = new HashSet<>();

        CachedEntity(CachedEntity parent) {
            this.id = ++g_sno;
            if (parent != null) {
                parents.add(parent);
            }
        }

        final boolean addParent(CachedEntity parent) {
            if (!this.parents.contains(parent)) {
                parents.add(parent);
                return true;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            return o == this;
        }
    }
}

