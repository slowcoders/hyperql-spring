package org.slowcoders.hyperql.jdbc.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slowcoders.hyperql.EntitySet;
import org.slowcoders.hyperql.jdbc.output.BatchPreparedStatementSetterWithKeyHolder;
import org.slowcoders.hyperql.schema.QColumn;
import org.slowcoders.hyperql.schema.QSchema;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BatchUpsert<ID> implements BatchPreparedStatementSetterWithKeyHolder {
    private final Map<String, Object>[] entities;
    private final List<QColumn> columns;
    private final String sql;
    private final QSchema schema;
    private List<Map<String, Object>> generatedKeys;
    private static final boolean USE_FLAT_KEY = false;

    private static ObjectMapper objectMapper = new ObjectMapper();

    private BatchUpsert(JdbcSchema schema, Collection<Map<String, Object>> entities, EntitySet.InsertPolicy insertPolicy) {
        QueryGenerator gen = schema.getStorage().createQueryGenerator();
        List<QColumn> columns = schema.getWritableColumns();
        this.entities = entities.toArray(new Map[entities.size()]);
        this.schema = schema;
        if (this.entities.length == 1) {
            columns = new ArrayList<>();
            if (USE_FLAT_KEY) {
                for (String key : this.entities[0].keySet()) {
                    columns.add((JdbcColumn) schema.getColumn(key));
                }
            } else {
                extractMappedColumn(columns, this.entities[0], "");
            }
        }
        this.sql = gen.prepareBatchInsertStatement(schema, (List)columns, insertPolicy);
        this.columns = columns;
    }

    private void extractMappedColumn(List<QColumn> columns, Map<String, Object> columnMap, String base_key) {
        for (String key : columnMap.keySet()) {
            Object value = columnMap.get(key);
            if (value instanceof Map) {
                QColumn col = schema.findColumn(base_key + key);
                if (col == null) {
                    extractMappedColumn(columns, (Map<String, Object>) value, key + '.');
                    continue;
                }
            }
            columns.add((JdbcColumn) schema.getColumn(base_key + key));
        }
    }

    public static <ID> List<ID> execute(JdbcTemplate jdbc, JdbcSchema schema, Collection<? extends Map<String, Object>> entities, EntitySet.InsertPolicy insertPolicy) {
        if (schema.hasGeneratedId()) {
            // insertPolicy == JqlEntitySet.InsertPolicy.ErrorOnConflict
            for (Map<String, Object> entity : entities) {
                if (schema.getEnityId(entity) != null) {
                    throw new IllegalArgumentException("Entity can not be created with generated id");
                }
            }
        }
        BatchUpsert batch = new BatchUpsert(schema, entities, insertPolicy);
        BatchPreparedStatementSetterWithKeyHolder.batchUpdateWithKeyHolder(jdbc, batch.getSql(), batch);
        return batch.getEntityIDs();
    }

    public String getSql() {
        return sql;
    }

    @Override
    public void setValues(PreparedStatement ps, int i) throws SQLException {
        Map<String, Object> entity = entities[i];
        int idx = 0;
        for (QColumn col : columns) {
            String jsKey = col.getJsonKey();
            Object json_v = null;
            if (USE_FLAT_KEY) {
                json_v = entity.get(jsKey);
            } else {
                int dot_p = jsKey.indexOf('.');
                if (dot_p > 0) {
                    String k1 = jsKey.substring(0, dot_p);
                    Object sub = entity.get(k1);
                    if (sub instanceof Map) {
                        json_v = ((Map) sub).get(jsKey.substring(dot_p + 1));
                    }
                }
                if (json_v == null) {
                    json_v = entity.get(jsKey);
                }
            }
            if (json_v == null) {
                json_v = entity.get(col.getPhysicalName());
            }
            Object value = convertJsonValueToColumnValue(col, json_v);
            ps.setObject(++idx, value);
        }
        ps.getGeneratedKeys();
    }

    static Object convertJsonValueToColumnValue(QColumn col, Object v) {
        if (v == null) return null;

        if (col.isJsonNode()) {
            if ((v instanceof Map) || (v instanceof Collection)) {
                try {
                    v = objectMapper.writeValueAsString(v);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            return v.toString();
        }

        if (v.getClass().isEnum()) {
            if (Number.class.isAssignableFrom(col.getValueType())) {
                return ((Enum) v).ordinal();
            } else {
                return v.toString();
            }
        }
        return v;
    }


    @Override
    public int getBatchSize() {
        return entities.length;
    }

    @Override
    public void setGeneratedKeys(List<Map<String, Object>> keys) {
        this.generatedKeys = keys;
    }

    public List<ID> getEntityIDs() {
        int cntKeys = generatedKeys == null ? 0 : generatedKeys.size();
        List<QColumn> pkColumns = schema.getPKColumns();
        ArrayList<ID> ids = new ArrayList<>();
        for (int i = 0; i < entities.length; i++) {
            ID id = (ID)extractEntityId(pkColumns, entities[i], i < cntKeys ? this.generatedKeys.get(i) : null);
            ids.add(id);
        }
        return ids;
    }

    private static Object extractEntityId(List<QColumn> pkColumns, Map<String, Object> entity, Map<String, Object> generatedKeys) {
        if (pkColumns.size() > 1) {
            Object[] id = new Object[pkColumns.size()];
            int i = 0;
            for (QColumn pk : pkColumns) {
                Object v = getValue(pk.getJsonKey(), entity, generatedKeys);
                if (v == null) {
                    v = getValue(pk.getPhysicalName(), entity, generatedKeys);
                }
                id[i++] = v;
            }
            return id;
        }
        else {
            Object id = getValue(pkColumns.get(0).getJsonKey(), entity, generatedKeys);
            return id;
        }
    }

    private static Object getValue(String key, Map<String, Object> entity, Map<String, Object> generatedKeys) {
        if (generatedKeys != null && generatedKeys.containsKey(key)) {
            return generatedKeys.get(key);
        }
        return entity.get(key);
    }


}
