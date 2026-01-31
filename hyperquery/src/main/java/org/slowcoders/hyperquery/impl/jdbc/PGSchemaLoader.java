package org.slowcoders.hyperquery.impl.jdbc;


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class PGSchemaLoader extends JdbcSchemaLoader {


    public PGSchemaLoader(Connection conn) throws SQLException {
        super(conn.getCatalog(), conn.getSchema(), true);
    }


    public String getTableComment(String tableName) {
        TablePath tablePath = makeTablePath(tableName);
        String comment = null;
        // not implemented yet
        return comment;
    }


    protected Map<String, String> getColumnComments(Connection conn, TablePath tablePath) throws SQLException {
        HashMap<String, String> comments = new HashMap<>();
        String sql = "SELECT c.column_name, pgd.description\n" +
                "FROM information_schema.columns c\n" +
                "    inner join pg_catalog.pg_statio_all_tables as st on (c.table_name = st.relname)\n" +
                "    inner join pg_catalog.pg_description pgd on (pgd.objoid=st.relid and\n" +
                "          pgd.objsubid=c.ordinal_position)\n" +
                "where c.table_schema = '" + tablePath.getSchema() + "' and c.table_name = '" + tablePath.getSimpleName() + "';\n";

        ResultSet rs = conn.createStatement().executeQuery(sql);
        while (rs.next()) {
            String columnName = rs.getString("column_name");
            String comment = rs.getString("description");
            comments.put(columnName, comment);
        }
        return comments;
    }

//    public String createDDL(QSchema schema) {
////        SQLWriter sb = new SQLWriter(schema);
////        sb.write("const " + schema.getTableName() + "Schema = [\n");
////        for (JQColumn col : schema.getColumns()) {
////            sb.write(col.dumpJSONSchema()).write(",\n");
////        }
////        sb.write("]\n");
////        return sb.toString();
//        throw new RuntimeException("not implemented");
//    }
//
//    public SqlGenerator createSqlGenerator(boolean isNativeQuery) {
//        return new PGSqlGenerator(isNativeQuery);
//    }
}
