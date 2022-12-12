package org.eipgrid.jql.jdbc;

import org.eipgrid.jql.JqlColumn;
import org.eipgrid.jql.util.KVEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.JdbcUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JqlRowMapper implements ResultSetExtractor<List<KVEntity>> {
    private final List<JqlResultMapping> resultMappings;

    public JqlRowMapper(List<JqlResultMapping> rowMappings) {
        this.resultMappings = rowMappings;
    }

    @Override
    public List<KVEntity> extractData(ResultSet rs) throws SQLException, DataAccessException {
        MappedColumn[] mappedColumns = initMappedColumns(rs);

        KVEntity baseEntity = null;
        ArrayList<KVEntity> results = new ArrayList<>();

        while (rs.next()) {
            int idxColumn = 0;
            int columnCount = mappedColumns.length;
            if (baseEntity != null) {
                int lastMappingIndex = resultMappings.size() - 1;
                check_duplicated_columns:
                for (int i = 0; i < lastMappingIndex; i++) {
                    JqlResultMapping mapping = resultMappings.get(i);
                    List<JqlColumn> columns = mapping.getSelectedColumns();
                    if (columns.size() == 0) continue;
                    List<JqlColumn> pkColumns = mapping.getSchema().getPKColumns();
                    int pkIndex = idxColumn;
                    for (int pkCount = pkColumns.size(); --pkCount >= 0; pkIndex++) {
                        Object value = getColumnValue(rs, pkIndex + 1);
                        if (value == null) {
                            columnCount = idxColumn;
                            break check_duplicated_columns;
                        }
                        if (!value.equals(mappedColumns[pkIndex].value)) {
                            break check_duplicated_columns;
                        }
                    }
                    idxColumn += columns.size();
                }
            }

            if (idxColumn == 0) {
                baseEntity = new KVEntity();
                results.add(baseEntity);
            }
            KVEntity currEntity = baseEntity;
            JqlResultMapping currMapping = mappedColumns[0].mapping;
            for (; idxColumn < columnCount; ) {
                MappedColumn mappedColumn = mappedColumns[idxColumn];
                if (currMapping != mappedColumn.mapping) {
                    currMapping = mappedColumn.mapping;
                    currEntity = baseEntity;
                    String[] entityPath = currMapping.getEntityMappingPath();
                    int idxLastPath = entityPath.length - 1;
                    for (int i = 0; i < idxLastPath; i++) {
                        currEntity = makeSubEntity(currEntity, entityPath[i], false);
                    }
                    currEntity = makeSubEntity(currEntity, entityPath[idxLastPath], currMapping.isArrayNode());
                }
                Object value = getColumnValue(rs, ++idxColumn);
                mappedColumn.value = value;

                putValue(currEntity, mappedColumn, value);
            }
        }
        return results;
    }

    private static void putValue(KVEntity entity, MappedColumn mappedColumn, Object value) {
        KVEntity node = entity;
        JqlColumn column = mappedColumn.jqlColumn;
        String fieldName = column.getJavaFieldName();
        for (JqlColumn pk; (pk = column.getJoinedPrimaryColumn()) != null; ) {
            KVEntity pkEntity = (KVEntity) node.get(fieldName);
            if (pkEntity == null) {
                pkEntity = new KVEntity();
                node.put(fieldName, pkEntity);
            }
            node = pkEntity;
            column = pk;
            fieldName = column.getJavaFieldName();
        }

        Object old = node.put(fieldName, value);
        if (old != null && !old.equals(value)) {
            throw new RuntimeException("something wrong");
        }
    }

    private KVEntity makeSubEntity(KVEntity entity, String key, boolean isArray) {
        Object subEntity = entity.get(key);
        if (subEntity == null) {
            subEntity = new KVEntity();
            if (isArray) {
                ArrayList<Object> array = new ArrayList<>();
                array.add(subEntity);
                entity.put(key, array);
            } else {
                entity.put(key, subEntity);
            }
        } else if (isArray) {
            if (subEntity instanceof KVEntity) {
                /** TODO remove this tricky code */
                ArrayList<Object> array = new ArrayList<>();
                entity.put(key, array);
                array.add(subEntity);
            } else {
                ArrayList<Object> array = (ArrayList<Object>) subEntity;
                subEntity = new KVEntity();
                array.add(subEntity);
            }
        } else if (subEntity instanceof ArrayList) {
            ArrayList<KVEntity> list = (ArrayList<KVEntity>) subEntity;
            subEntity = list.get(list.size()-1);
        }
        return (KVEntity)subEntity;
    }

    protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
        return JdbcUtils.getResultSetValue(rs, index);
    }


    private MappedColumn[] initMappedColumns(ResultSet rs) throws SQLException {

        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();

        MappedColumn[] mappedColumns = new MappedColumn[columnCount];
        ColumnMappingHelper helper = new ColumnMappingHelper();

        int idxColumn = 0;
        for (JqlResultMapping mapping : resultMappings) {
            List<JqlColumn> columns = mapping.getSelectedColumns();
            if (columns.size() == 0) {
                continue;
            }
            helper.reset(mapping.getEntityMappingPath());
            for (JqlColumn column : columns) {
                String[] path = helper.getEntityMappingPath(column);
                mappedColumns[idxColumn++] = new MappedColumn(mapping, column, path);
            }
        }
        if (idxColumn != columnCount) {
            throw new RuntimeException("Something wrong!");
        }
        return mappedColumns;
    }


    private static class ColumnMappingHelper extends HashMap<String, ColumnMappingHelper> {
        String[] entityPath;

        void reset(String[] jsonPath) {
            this.entityPath = jsonPath;
            this.clear();
        }

        public String[] getEntityMappingPath(JqlColumn column) {
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
        final JqlColumn jqlColumn;
        final JqlResultMapping mapping;
        final String[] mappingPath;
        private Object   value;

        public MappedColumn(JqlResultMapping mapping, JqlColumn column, String[] path) {
            this.mapping = mapping;
            this.jqlColumn = column;
            this.mappingPath = path;
        }
    }
}
