package org.slowcoders.hyperql.jdbc.output;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slowcoders.hyperql.RestTemplate;
import org.slowcoders.hyperql.js.JsType;
import org.slowcoders.hyperql.js.JsUtil;
import org.slowcoders.hyperql.schema.QColumn;
import org.slowcoders.hyperql.schema.QResultMapping;
import org.springframework.dao.DataAccessException;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ArrayRowMapper implements JdbcResultMapper<Object[]> {
    private final List<QResultMapping> resultMappings;
    private final Properties properties;
    private final ObjectMapper objectMapper;
    private String[] columnNames;
    private boolean[] mappedColumns;

    public ArrayRowMapper(List<QResultMapping> rowMappings, Properties properties, ObjectMapper objectMapper) {
        this.resultMappings = rowMappings;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Object[]> extractData(ResultSet rs) throws SQLException, DataAccessException {

        String[] columnNames = initMappedColumns(rs);
        this.columnNames = columnNames;
        int columnCount = columnNames.length;

        ArrayList<Object[]> rows = new ArrayList<>();
        while (rs.next()) {
            Object[] values = new Object[columnCount];
            for (int i = columnNames.length; --i >= 0;) {
                Object value;
                if (mappedColumns[i]) {
                    value = JsUtil.parseJson(objectMapper, rs.getString(i + 1));
                } else {
                    value = rs.getObject(i+1);
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

                    */
                }
                values[i] = value;
            }
            rows.add(values);
        }
        if (this.properties != null) {
            this.properties.put("columnNames", columnNames);
        }
        return rows;
    }

    public String[] getColumnNames() {
        return this.columnNames;
    }

    private String[] initMappedColumns(ResultSet rs) throws SQLException {

        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();

        String[] columnNames = new String[columnCount];
        boolean[] columnTypes = new boolean[columnCount];

        for (int idxColumn = 0; idxColumn < columnCount; idxColumn++) {
            columnNames[idxColumn] = rsmd.getColumnName(idxColumn + 1);
            columnTypes[idxColumn] = rsmd.getColumnTypeName(idxColumn + 1).startsWith("json");
        }
        this.mappedColumns = columnTypes;
        return columnNames;
    }

    private String toJsonKey(String baseKey, String key) {
        if (baseKey.length() == 0) return key;
        return baseKey + '.' + key;
    }

    private String toJsonKey(String[] mappingPath) {
        StringBuilder sb = new StringBuilder();
        for (String s : mappingPath) {
            if (sb.length() > 0) sb.append('.');
            sb.append(s);
        }
        return sb.toString();
    }

    @Override
    public void setOutputMetadata(RestTemplate.Response response) {
        response.setProperty("columnNames", this.columnNames);
    }
}
