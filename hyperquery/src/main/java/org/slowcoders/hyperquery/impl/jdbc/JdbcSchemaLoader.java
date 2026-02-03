package org.slowcoders.hyperquery.impl.jdbc;

import jakarta.persistence.Table;
import org.springframework.dao.DataAccessException;

import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

public abstract class JdbcSchemaLoader {

//    protected final JdbcStorage storage;
    private final String defaultCatalog;

    private final String defaultSchema;

    private final boolean schemaSupported;

    public JdbcSchemaLoader(String defaultCatalog, String defaultSchema, boolean schemaSupported) {
        this.schemaSupported = schemaSupported;
        this.defaultCatalog = defaultCatalog;
        this.defaultSchema = defaultSchema;
//        == null ? ""
//        if (defaultSchema != null && defaultSchema.trim().length() == 0) {
//            defaultSchema = null;
//        }
//        this.defaultSchema = defaultSchema;
    }

//    public abstract SqlGenerator createSqlGenerator(boolean isNativeQuery);

//    public JdbcSchema loadSchema(Connection conn, TablePath tablePath, Class<?> ormType) throws SQLException {
//        String qname = tablePath.getQualifiedName();
//        JdbcSchema schema = new JdbcSchema(storage, qname, ormType);
//
//        ArrayList<String> primaryKeys = getPrimaryKeys(conn, tablePath);
//        HashMap<String, ArrayList<String>> uniqueConstraints = getUniqueConstraints(conn, tablePath);
//        if (primaryKeys.size() == 0) {
//            for (ArrayList<String> keys : uniqueConstraints.values()) {
//                if (primaryKeys.size() == 0 || keys.size() < primaryKeys.size()) {
//                    primaryKeys = keys;
//                }
//            }
//        }
//        Map<String, String> comments = getColumnComments(conn, tablePath);
//        ArrayList<JdbcColumn> columns = getColumns(conn, comments, schema, primaryKeys);
//        processForeignKeyConstraints(conn, schema, tablePath, columns);
//        schema.init(columns, uniqueConstraints, ormType);
//        return schema;
//    }
//
//    public void initVirtualSchema(Connection conn, VirtualSchema schema, List<String> primaryKeys) throws SQLException {
//        ArrayList<JdbcColumn> columns = getColumns(conn, null, schema, primaryKeys);
////        processForeignKeyConstraints(conn, schema, tablePath, columns);
//        schema.init(columns, Collections.EMPTY_MAP, HyperRepository.rawEntityType);
//    }

    public ArrayList<String> getPrimaryKeys(Connection conn, TablePath tablePath) throws SQLException {
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getPrimaryKeys(tablePath.getCatalog(), tablePath.getSchema(), tablePath.getSimpleName());
        ArrayList<String> keys = new ArrayList<>();
        int next_key_seq = 1;
        while (rs.next()) {
            String key = rs.getString("column_name");
            if (false) {
                // postgresql 에서만 동작.
                int seq = rs.getInt("key_seq");
                if (seq != next_key_seq) {
                    throw new RuntimeException("something wrong");
                }
                next_key_seq++;
            }
            keys.add(key);
        }
        return keys;
    }

    public List<String> getNamespaces(Connection conn) throws SQLException, DataAccessException {
        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = schemaSupported ? md.getSchemas() : md.getCatalogs();
        ArrayList<String> names = new ArrayList<>();
        while (rs.next()) {
            String name = rs.getString(schemaSupported ? "TABLE_SCHEM": "TABLE_CAT");
            names.add(name);
        }
        return names;
    }


    public ArrayList<String> getTableNames(Connection conn, String namespace) throws SQLException {
        DatabaseMetaData md = conn.getMetaData();
        String[] types = {"TABLE"};
        ResultSet rs = md.getTables(namespace, namespace, "%", types);
        ArrayList<String> names = new ArrayList<>();
        while (rs.next()) {
            String name = rs.getString("TABLE_NAME");
            names.add(name);
        }
        return names;
    }

    public HashMap<String, ArrayList<String>> getUniqueConstraints(Connection conn, TablePath tablePath) throws SQLException {
        HashMap<String, ArrayList<String>> indexMap = new HashMap<>();

        DatabaseMetaData md = conn.getMetaData();
        ResultSet rs = md.getIndexInfo(tablePath.getCatalog(), tablePath.getSchema(), tablePath.getSimpleName(), true, false);
        while (rs.next()) {
            String table_schem = rs.getString("table_schem");
            String table_name = rs.getString("table_name");
            String index_qualifier = rs.getString("index_qualifier");
            String index_name = rs.getString("index_name");
            String column_name = rs.getString("column_name");
            String filter_condition = rs.getString("filter_condition");
            boolean is_unique = !rs.getBoolean("non_unique");
            String sort = rs.getString("asc_or_desc");
            int type = rs.getInt("type");
            int ordinal_position = rs.getInt("ordinal_position");
            int cardinality = rs.getInt("cardinality");
            int pages = rs.getInt("pages");

            String table_cat = rs.getString("table_cat");
            assert(is_unique);

            ArrayList<String> indexes = indexMap.get(index_name);
            if (indexes == null) {
                indexes = new ArrayList<>();
                indexMap.put(index_name, indexes);
            }
            indexes.add(column_name);
        }
        return indexMap;
    }

    private JdbcColumn getColumnByPhysicalName(ArrayList<JdbcColumn> columns, String columnName) {
        columnName = columnName.toLowerCase();
        for (JdbcColumn column : columns) {
            if (columnName.equals(column.getPhysicalName().toLowerCase())) {
                return column;
            }
        }
        throw new RuntimeException("column not found: " + columnName);
    }

//    private void processForeignKeyConstraints(Connection conn, JdbcSchema fkSchema, TablePath tablePath, ArrayList<JdbcColumn> columns) throws SQLException {
//        DatabaseMetaData md = conn.getMetaData();
//        ResultSet rs = md.getImportedKeys(tablePath.getCatalog(), tablePath.getSchema(), tablePath.getSimpleName());
//        while (rs.next()) {
//            JoinData join = new JoinData(rs, this);
//            JdbcColumn fk = getColumnByPhysicalName(columns, join.fkColumnName);
//            fk.bindPrimaryKey(new ColumnBinder(storage, join.pkTableQName, join.pkColumnName));
//            fkSchema.addForeignKeyConstraint(join.fk_name, fk);
//        }
//    }
//
//    public void loadExternalJoins(Connection conn, JdbcSchema pkSchema) throws SQLException {
//        Map<String, QJoin> res = pkSchema.getEntityJoinMap(false);
//        if (res != null) {
//            return;
//        }
//
//        final TablePath tablePath = makeTablePath(pkSchema.getTableName());
//        DatabaseMetaData md = conn.getMetaData();
//        ResultSet rs = md.getExportedKeys(tablePath.getCatalog(), tablePath.getSchema(), tablePath.getSimpleName());
//
//        JoinMap.Builder joins = new JoinMap.Builder(pkSchema);
//        while (rs.next()) {
//            JoinData join = new JoinData(rs, this);
//            JdbcSchema fkSchema = (JdbcSchema)storage.loadSchema(join.fkTableQName);
//            QJoin fkJoin = fkSchema.getJoinByForeignKeyConstraints(join.fk_name);
//            joins.put(fkSchema, fkJoin);
//        }
//        Map<String, QJoin> joinMap = joins.createJoinMap((JdbcSchema) pkSchema);
//        pkSchema.setEntityJoinMap(joinMap);
//    }

    public ArrayList<JdbcColumn> getColumns(Connection conn, Map<String, String> comments, TablePath tablePath, List<String> primaryKeys) throws SQLException {
        //HashMap<String, JqlIndex> indexes = getUniqueConstraints(conn, dbSchema, tableName);
        ArrayList<JdbcColumn> columns = new ArrayList<>();
        String sql = "select * from " + tablePath.getQualifiedName() + " where false";
        ResultSet rs = conn.createStatement().executeQuery(sql);
        ResultSetMetaData md = rs.getMetaData();
        int cntColumn = md.getColumnCount();
        String comment = null;
        for (int col = 0; ++col <= cntColumn; ) {
            String columnName = md.getColumnName(col);
            if (comments != null) {
                comment = comments.get(columnName);
                if ("<deprecated>".equals(comment)) {
                    continue;
                }
            }
            JdbcColumn ci = new JdbcColumn(md, col, comment, primaryKeys);
            columns.add(ci);
        }
        return columns;
    }

    public abstract String getTableComment(String tableName);

    protected abstract Map<String, String> getColumnComments(Connection conn, TablePath tablePath) throws SQLException;


    public void dumResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        while (rs.next()) {
            for (int i = 0; ++i <= colCount; ) {
                String colName = meta.getColumnName(i);
                String value = rs.getString(i);
                System.out.println(colName + ": " + value);
            }
        }
    }


    public TablePath getTablePath(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        return table != null ? makeTablePath(table, this.schemaSupported) : null;
    }

    static class JoinData {
        String pk_name;
        String pktable_schem;
        String pktable_name;

        // 참고) fk_name 은 column-name 이 아니라, fk constraint 의 name 이다.
        String fk_name;
        String fktable_schem;
        String fktable_name;

        String pkColumnName;
        String fkColumnName;
        String fkTableQName;
        String pkTableQName;

        int key_seq;
        int update_rule;
        int delete_rule;
        int deferrability;
        String pktable_cat;
        String fktable_cat;

        JoinData(ResultSet rs, JdbcSchemaLoader loader) throws SQLException {
            this.pk_name = rs.getString("pk_name");
            this.pktable_schem = rs.getString("pktable_schem");
            this.pktable_name  = rs.getString("pktable_name");
            this.pktable_cat = rs.getString("pktable_cat");

            // 참고) fk_name 은 column-name 이 아니라, fk constraint 의 name 이다.
            this.fk_name = rs.getString("fk_name");
            this.fktable_schem = rs.getString("fktable_schem");
            this.fktable_name  = rs.getString("fktable_name");
            this.fktable_cat = rs.getString("fktable_cat");

            this.pkColumnName = rs.getString("pkcolumn_name");
            this.fkColumnName = rs.getString("fkcolumn_name");
            this.fkTableQName = loader.makeQualifiedName(fktable_cat, fktable_schem, fktable_name);
            this.pkTableQName = loader.makeQualifiedName(pktable_cat, pktable_schem, pktable_name);

            this.key_seq = rs.getInt("key_seq");
            this.update_rule = rs.getInt("update_rule");
            this.delete_rule = rs.getInt("delete_rule");
            this.deferrability = rs.getInt("deferrability");
        }
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }

    private static final Pattern lowercase_and_numbers = Pattern.compile("[a-z_$][a-z0-9_$]*");
    private static String toNameToken(String s) {
        if (s.charAt(0) == '"') {
            assert (s.charAt(s.length() - 1) == '"');
        }
        else if (!lowercase_and_numbers.matcher(s).matches()) {
            assert (s.charAt(s.length() - 1) != '"');
            s = '"' + s + '"';
        }
        return s;
    }


    private static String combineDBNameTokens(String db_catalog, String db_schema, String table_name) {
        StringBuilder sb = new StringBuilder();
        if (!isEmpty(db_catalog)) {
            sb.append(toNameToken(db_catalog)).append('.');
        }
        if (!isEmpty(db_schema)) {
            sb.append(toNameToken(db_schema)).append('.');
        }
        sb.append(toNameToken(table_name));
        return sb.toString();
    }

    private String makeQualifiedName(String db_catalog, String db_schema, String table_name) {
        if (isEmpty(db_catalog)) db_catalog = this.defaultCatalog;
        if (isEmpty(db_schema)) db_schema = this.defaultSchema;
        return combineDBNameTokens(db_catalog, db_schema, table_name);
    }

    public TablePath makeTablePath(Table table, boolean useSchema) {
        String catalog = useSchema ? this.defaultCatalog: table.catalog();
        String schema = useSchema ? table.schema() : this.defaultSchema;
        String tableName = table.name();

        return new TablePath(catalog, schema, tableName);

//        String name = table.name();
//        String namespace = useSchema ? table.schema() : table.catalog();
//        if (namespace.length() == 0) {
//            namespace = this.defaultSchema;
//        }
//        return TablePath.of(namespace, name);
    }

    public TablePath makeTablePath(String tableName) {
        tableName = tableName.toLowerCase();
        int last_dot_p = tableName.lastIndexOf('.');
        int first_dot_p = tableName.lastIndexOf('.', last_dot_p - 1);
        String simpleName = tableName.substring(last_dot_p + 1);
        String schema = last_dot_p < 0 || !schemaSupported ? this.defaultSchema : tableName.substring(first_dot_p + 1, last_dot_p);
        String catalog = first_dot_p < 0 ? this.defaultCatalog : tableName.substring(0, first_dot_p);
        return new TablePath(catalog, schema, simpleName);
    }

    public static class TablePath {
        private final String catalog;
        private final String schema;
        private final String qualifiedName;
        private final String simpleName;

        TablePath(String catalog, String schema, String simpleName) {
            this.catalog = unquoteToken(catalog);
            this.schema = unquoteToken(schema);
            this.simpleName = unquoteToken(simpleName);
            this.qualifiedName = combineDBNameTokens(catalog, schema, simpleName);
        }

        public String getQualifiedName() {
            return qualifiedName;
        }

        public String getSimpleName() {
            return simpleName;
        }

        public String getCatalog() {
            return catalog;
        }

        public String getSchema() {
            return schema;
        }

        private static String unquoteToken(String s) {
            if (s != null && s.length() > 0) {
                if (s.charAt(0) == '"') {
                    assert (s.charAt(s.length() - 1) == '"');
                    s = s.substring(1, s.length() - 1);
                }
                assert (s.charAt(0) != '"');
                assert (s.charAt(s.length() - 1) != '"');
            }
            return s;
        }
    }
}
