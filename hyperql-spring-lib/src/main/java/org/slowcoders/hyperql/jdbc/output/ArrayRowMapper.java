package org.slowcoders.hyperql.jdbc.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slowcoders.hyperql.RestTemplate;
import org.slowcoders.hyperql.jdbc.JdbcQuery;
import org.slowcoders.hyperql.js.JsUtil;
import org.slowcoders.hyperql.js.JsonObject;
import org.slowcoders.hyperql.util.KVEntity;
import org.springframework.dao.DataAccessException;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

public class ArrayRowMapper implements JdbcResultMapper<Object[]> {
    private final JdbcQuery query;
    private final ObjectMapper objectMapper;
    private boolean[] jsonColumns;

    public ArrayRowMapper(JdbcQuery query, ObjectMapper objectMapper) {
//        this.resultMappings = rowMappings;
        this.query = query;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Object[]> extractData(ResultSet rs) throws SQLException, DataAccessException {

        String[] columnNames = initMappedColumns(rs);
//        this.columnNames = columnNames;
        int columnCount = columnNames.length;

        ArrayList<Object[]> rows = new ArrayList<>();
        while (rs.next()) {
            Object[] values = new Object[columnCount];
            for (int i = columnCount; --i >= 0;) {
                Object value = rs.getObject(i+1);
                if (value != null && jsonColumns[i]) {
                    value = new JsonObject(value.toString());
                } else {
                    /*
                    if (value != null) {
                        JsType type = JsType.of(value.getClass());
                        if (type == JsType.Object) {
                            try {
                                value = objectMapper.readValue(value.toString(), JsonNode.class);
                            } catch (JsonProcessingException e) {
                                // mariadb longtext 인 경우;
                            }
                        }
                    }
                    //*/
                }
                values[i] = value;
            }
            rows.add(values);
        }
        return rows;
    }

    public static List<KVEntity> extractJsonData(List<Object[]> rows, ArrayList<Object> nameMappings)  {
        RowToJsonConverter jsConvertor = new RowToJsonConverter(nameMappings);
        List<KVEntity> jsRows = new ArrayList<>();
        for (Object[] row : rows) {
            KVEntity entity = jsConvertor.convert(row);
            jsRows.add(entity);
        }
        return jsRows;
    }

    static class RowToJsonConverter {
        private final ArrayList<Object> nameMappings;
        private MapTarget target = new MapTarget();
        private final ObjectMapper objectMapper;
        static class MapTarget {
            KVEntity entity;
            String key;
        }

        public RowToJsonConverter(ArrayList<Object> nameMappings) {
            this.nameMappings = nameMappings;
            this.objectMapper = new ObjectMapper();
        }

        public KVEntity convert(Object[] row) {
            KVEntity entity = extractJson(nameMappings.iterator(), Arrays.asList(row), 0);
            return entity;
        }

        private boolean resolveScope(MapTarget target, KVEntity entity, Object name) {
            if (name instanceof String key) {
                target.entity = entity;
                target.key = key;
            } else if (name instanceof String[] keys) {
                for (int idx = 0; idx < keys.length - 1; idx++) {
                    KVEntity subEntity = (KVEntity) entity.get(keys[idx]);
                    if (subEntity == null) {
                        entity.put(keys[idx], subEntity = new KVEntity());
                    }
                    entity = subEntity;
                }
                target.entity = entity;
                target.key = keys[keys.length - 1];
            } else {
                return false;
            }
            return true;
        }

        private KVEntity extractJson(Iterator<Object> pathIterator, List<Object> row, int idxColumn) {
            KVEntity entity = new KVEntity();
            while (pathIterator.hasNext()) {
                Object name = pathIterator.next();
                Object value = row.get(idxColumn++);

                if (this.resolveScope(this.target, entity, name)) {
                    this.target.entity.put(this.target.key, value);
                } else if (name instanceof Map map) {
                    if (!(value instanceof List<?>)) {
                        value = JsUtil.parseJson(objectMapper, value.toString());
                    }

                    MapTarget subTarget = new MapTarget();
                    this.resolveScope(subTarget, entity, map.get("name"));
                    List<Object> columns = (List<Object>)map.get("columns");
                    var subRows = (List<List<Object>>) value;
                    List<KVEntity> subEntities = new ArrayList<>();
                    for (List<Object> subRow : subRows) {
                        var subEntity = extractJson(columns.iterator(), subRow, 0);
                        subEntities.add(subEntity);
                    }
                    subTarget.entity.put(subTarget.key, subEntities);
                }
            }
            return entity;
        }
    }

    private String[] initMappedColumns(ResultSet rs) throws SQLException {

        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();

        String[] columnNames = new String[columnCount];
        boolean[] jsonColumns = new boolean[columnCount];

        for (int idxColumn = 0; idxColumn < columnCount; idxColumn++) {
            columnNames[idxColumn] = rsmd.getColumnName(idxColumn + 1);
            jsonColumns[idxColumn] = rsmd.getColumnTypeName(idxColumn + 1).toLowerCase().startsWith("json");
        }
        this.jsonColumns = jsonColumns;
        return columnNames;
    }

    @Override
    public void setOutputMetadata(RestTemplate.Response response) {
        // response.setProperty("columnNames", this.query.getFilter().getColumnNameMappings());
    }
}
