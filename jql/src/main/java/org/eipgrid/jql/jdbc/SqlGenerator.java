package org.eipgrid.jql.jdbc;

import org.eipgrid.jql.JqlColumn;
import org.eipgrid.jql.JqlSchema;
import org.eipgrid.jql.JqlSchemaJoin;
import org.eipgrid.jql.parser.JqlQuery;
import org.eipgrid.jql.parser.QueryBuilder;
import org.eipgrid.jql.parser.SqlWriter;
import org.springframework.data.domain.Sort;

import java.util.*;

public class SqlGenerator implements QueryBuilder {

    private final SqlWriter sw;

    public SqlGenerator() {
        this(new SqlWriter());
    }

    public SqlGenerator(SqlWriter sqlWriter) {
        this.sw = sqlWriter;//new SqlWriter(schema, null);
    }

    protected String getCommand(SqlWriter.Command command) {
        return command.toString();
    }

    protected void writeWhere(JqlQuery where) {
        if (!where.isEmpty()) {
            sw.writeRaw("\nWHERE ");
            where.accept(sw);
        }
    }

    private void writeFrom(JqlQuery where) {
        writeFrom(where, where.getTableName(), false);
    }

    private void writeFrom(JqlQuery where, String tableName, boolean ignoreEmptyFilter) {
        sw.write("FROM ").write(tableName).write(" as ").write(where.getMappingAlias());
        for (JqlResultMapping fetch : where.getResultColumnMappings()) {
            JqlSchemaJoin join = fetch.getSchemaJoin();
            if (join == null) continue;

            if (ignoreEmptyFilter && !fetch.hasFilterPredicates()) continue;

            String parentAlias = fetch.getParentNode().getMappingAlias();
            String alias = fetch.getMappingAlias();
            if (true || join.isUniqueJoin()) {
                JqlSchemaJoin associated = join.getAssociativeJoin();
                writeJoinStatement(join, parentAlias, associated == null ? alias : "p" + alias);
                if (associated != null) {
                    writeJoinStatement(associated, "p" + alias, alias);
                }
            } else {

            }
        }
    }


    private void writeJoinStatement(JqlSchemaJoin joinKeys, String baseAlias, String alias) {
        boolean isInverseMapped = joinKeys.isInverseMapped();
        String joinedTable = joinKeys.getJoinedSchema().getTableName();
        sw.write("\nleft outer join ").write(joinedTable).write(" as ").write(alias).write(" on\n\t");
        for (JqlColumn fk : joinKeys.getForeignKeyColumns()) {
            JqlColumn anchor, linked;
            if (isInverseMapped) {
                linked = fk; anchor = fk.getJoinedPrimaryColumn();
            } else {
                anchor = fk; linked = fk.getJoinedPrimaryColumn();
            }
            sw.write(baseAlias).write(".").write(anchor.getColumnName());
            sw.write(" = ").write(alias).write(".").write(linked.getColumnName()).write(" and\n\t");
        }
        sw.shrinkLength(6);
    }

    public String createCountQuery(JqlQuery where) {
        sw.write("\nSELECT count(*) ");
        writeFrom(where);
        writeWhere(where);
        String sql = sw.reset();
        return sql;
    }

    private boolean needDistinctPagination(JqlQuery where) {
        for (JqlResultMapping mapping : where.getResultColumnMappings()) {
            JqlSchemaJoin join = mapping.getSchemaJoin();
            if (join == null) continue;

            if (mapping.hasFilterPredicates()) {
                return true;
            }
            if (mapping.isArrayNode() && mapping.getEntityMappingPath().length == 1) {
                return true;
            }
        }
        return false;
    }

    public String createSelectQuery(JqlQuery where, Sort sort, int offset, int limit) {
        sw.reset();
        String tableName = where.getTableName();

        boolean need_complex_pagination = (limit > 0 || offset > 0) && needDistinctPagination(where);
        if (need_complex_pagination) {
            sw.write("\nWITH _cte AS NOT MATERIALIZED (\n");
            sw.incTab();
            sw.write("SELECT DISTINCT t_0.* ");
            writeFrom(where, tableName, true);
            writeWhere(where);
            tableName = "_cte";
            write_orderBy(where, sort, offset, limit);
            sw.decTab();
            sw.write("\n)");
        }

        sw.write("\nSELECT\n");
        for (JqlResultMapping mapping : where.getResultColumnMappings()) {
            sw.write('\t');
            String alias = mapping.getMappingAlias();
            for (JqlColumn col : mapping.getSelectedColumns()) {
                sw.write(alias).write('.').write(col.getColumnName()).write(", ");
            }
            sw.write('\n');
        }
        sw.replaceTrailingComma("\n");
        writeFrom(where, tableName, false);
        writeWhere(where);
        if (!need_complex_pagination) {
            write_orderBy(where, sort, offset, limit);
        }

        String sql = sw.reset();
        return sql;
    }

    private void write_orderBy(JqlQuery where, Sort sort, int offset, int limit) {
        if (sort != null) {
            sw.write("\nORDER BY ");
            JqlSchema schema = where.getSchema();
            sort.forEach(order -> {
                String p = order.getProperty();
                sw.write(where.getMappingAlias()).write('.');
                sw.write(schema.getColumn(p).getColumnName());
                sw.write(order.isAscending() ? " asc" : " desc").write(", ");
            });
            sw.replaceTrailingComma("");
        }

        if (offset > 0) sw.write("\nOFFSET " + offset);
        if (limit > 0) sw.write("\nLIMIT " + limit);
    }


    public String createUpdateQuery(JqlQuery where, Map<String, Object> updateSet) {
        sw.write("\nUPDATE ").write(where.getTableName()).write(" SET\n");

        for (Map.Entry<String, Object> entry : updateSet.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            sw.write("  ");
            sw.write(key).write(" = ").writeValue(value);
            sw.write(",\n");
        }
        sw.replaceTrailingComma("\n");
        this.writeWhere(where);
        String sql = sw.reset();
        return sql;

    }

    public String createDeleteQuery(JqlQuery where) {
        sw.write("\nDELETE ");
        this.writeFrom(where);
        this.writeWhere(where);
        String sql = sw.reset();
        return sql;
    }

    public String prepareFindByIdStatement(JqlSchema schema) {
        sw.write("\nSELECT * FROM ").write(schema.getTableName()).write("\nWHERE ");
        List<JqlColumn> keys = schema.getPKColumns();
        for (int i = 0; i < keys.size(); ) {
            String key = keys.get(i).getColumnName();
            sw.write(key).write(" = ? ");
            if (++ i < keys.size()) {
                sw.write(" AND ");
            }
        }
        String sql = sw.reset();
        return sql;
    }

    public String createInsertStatement(JqlSchema schema, Map entity, boolean ignoreConflict) {

        Set<String> keys = ((Map<String, ?>)entity).keySet();
        sw.writeln();
        sw.write(getCommand(SqlWriter.Command.Insert)).write(" INTO ").write(schema.getTableName()).writeln("(");
        sw.incTab();
        for (String name : schema.getPhysicalColumnNames(keys)) {
            sw.write(name);
            sw.write(", ");
        }
        sw.shrinkLength(2);
        sw.decTab();
        sw.writeln("\n) VALUES (");
        for (String k : keys) {
            Object v = entity.get(k);
            sw.writeValue(v).write(", ");
        }
        sw.replaceTrailingComma(")");
        if (ignoreConflict) {
            sw.write("\nON CONFLICT DO NOTHING");
        }
        String sql = sw.reset();
        return sql;
    }

    public String prepareBatchInsertStatement(JqlSchema schema, boolean ignoreConflict) {
        sw.writeln();
        sw.write(getCommand(SqlWriter.Command.Insert)).write(" INTO ").write(schema.getTableName()).writeln("(");
        for (JqlColumn col : schema.getWritableColumns()) {
            sw.write(col.getColumnName()).write(", ");
        }
        sw.replaceTrailingComma("\n) VALUES (");
        for (int i = schema.getWritableColumns().size(); --i >= 0; ) {
            sw.write("?,");
        }
        sw.replaceTrailingComma(")");
        if (ignoreConflict) {
            sw.write("\nON CONFLICT DO NOTHING");
        }
        String sql = sw.reset();
        return sql;
    }

    public BatchUpsert prepareInsert(JqlSchema schema, Collection<Map<String, Object>> entities) {
        return prepareInsert(schema, entities, schema.getTableName(), true);
    }


    public BatchUpsert prepareInsert(JqlSchema schema, Collection<Map<String, Object>> entities, String extendedTableName, boolean ignoreConflict) {
        String sql = prepareBatchInsertStatement(schema, ignoreConflict);
        return new BatchUpsert(entities, schema, sql);
    }


}