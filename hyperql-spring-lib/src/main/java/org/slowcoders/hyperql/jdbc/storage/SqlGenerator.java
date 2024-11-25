package org.slowcoders.hyperql.jdbc.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slowcoders.hyperql.EntitySet;
import org.slowcoders.hyperql.jdbc.JdbcQuery;
import org.slowcoders.hyperql.HyperQuery;
import org.slowcoders.hyperql.jdbc.SqlWriter;
import org.slowcoders.hyperql.schema.QJoin;
import org.slowcoders.hyperql.schema.QResultMapping;
import org.slowcoders.hyperql.js.JsType;
import org.slowcoders.hyperql.schema.*;
import org.slowcoders.hyperql.parser.HyperFilter;
import org.slowcoders.hyperql.parser.EntityFilter;
import org.springframework.data.domain.Sort;

import java.util.*;

public abstract class SqlGenerator extends SqlConverter implements QueryGenerator {

    private final boolean isNativeQuery;
    private EntityFilter currentNode;

    private List<QResultMapping> resultMappings;
    private boolean noAliases;

    private ObjectMapper objectMapper = new ObjectMapper();
    private static final boolean USE_FLAT_KEY = false;

    public SqlGenerator(boolean isNativeQuery) {
        super(new SqlWriter());
        this.isNativeQuery = isNativeQuery;
    }

    protected String getCommand(SqlConverter.Command command) {
        return command.toString();
    }

    protected void writeFilter(EntityFilter hql) {
        currentNode = hql;
        if (hql.visitPredicates(this)) {
            sw.write(" AND ");
        }
        for (EntityFilter child : hql.getChildNodes()) {
            if (!child.isEmpty()) {
                writeFilter(child);
            }
        }
    }

    protected void writeQualifiedColumnName(QColumn column, Object value) {
        if (!currentNode.isJsonNode()) {
            String name = isNativeQuery ? column.getPhysicalName() : column.getJsonKey();
            if (!noAliases) {
                sw.write(this.currentNode.getMappingAlias()).write('.');
            }
            sw.write(name);
            if (column.isJsonNode()) {
                JsType valueType = value == null ? null : JsType.of(value.getClass());
                writeTypeCast(valueType);
            }
        }
        else {
            JsType valueType = value == null ? null : JsType.of(value.getClass());
            writeJsonPath(currentNode, column, valueType);
        }
    }

    protected void writeTypeCast(JsType vf) {
        // do nothing.. postgresql 전용으로 사용되고 있음....
    }


    protected abstract void writeJsonPath(EntityFilter node, QColumn column, JsType valueType);

    protected void writeWhere(HyperFilter where) {
        if (!where.isEmpty()) {
            sw.write("\nWHERE ");
            int len = sw.length();
            writeFilter(where);
            if (sw.length() == len) {
                // no conditions.
                sw.shrinkLength(7);
            }
            else if (sw.endsWith(" AND ")) {
                sw.shrinkLength(5);
            }
        }
    }

    private void writeFrom(HyperFilter where, String tableName, boolean ignoreEmptyFilter) {
        sw.write("FROM ").write(tableName);
        if (!noAliases) {
            sw.write(isNativeQuery ? " as " : " ").write(where.getMappingAlias());
        }
        for (QResultMapping fetch : this.resultMappings) {
            QJoin join = fetch.getEntityJoin();
            if (join == null) continue;

            if (ignoreEmptyFilter && fetch.isEmpty()) continue;

            String parentAlias = fetch.getParentNode().getMappingAlias();
            String alias = fetch.getMappingAlias();
            if (isNativeQuery) {
                QJoin associated = join.getAssociativeJoin();
                writeJoinStatement(join, parentAlias, associated == null ? alias : "p" + alias);
                if (associated != null) {
                    writeJoinStatement(associated, "p" + alias, alias);
                }
            }
            else {
                sw.write((fetch.getSelectedColumns().size() > 0 || fetch.hasChildMappings()) ? " join fetch " : " join ");
                sw.write(parentAlias).write('.').write(join.getJsonKey()).write(" ").write(alias).write("\n");
            }
        }

//        if (!isNativeQuery) {
//            sw.replaceTrailingComma("");
//        }
    }


    private void writeJoinStatement(QJoin join, String baseAlias, String alias) {
        boolean isInverseMapped = join.isInverseMapped();
        String mediateTable = join.getLinkedSchema().getTableName();
        sw.write("\nleft join ").write(mediateTable).write(" as ").write(alias).write(" on\n\t");
        for (QColumn fk : join.getJoinConstraint()) {
            QColumn anchor, linked;
            if (isInverseMapped) {
                linked = fk; anchor = fk.getJoinedPrimaryColumn();
            } else {
                anchor = fk; linked = fk.getJoinedPrimaryColumn();
            }
            sw.write(baseAlias).write(".").write(anchor.getPhysicalName());
            sw.write(" = ").write(alias).write(".").write(linked.getPhysicalName()).write(" and\n\t");
        }
        sw.shrinkLength(6);
    }

    public String createCountQuery(HyperFilter where, String[] viewParams) {
        this.resultMappings = where.getResultMappings();
        sw.write("\nSELECT count(*) ");
        this.writeFrom(where, where.getTableExpression(viewParams), false);
        writeWhere(where);
        String sql = sw.reset();
        return sql;
    }

    private boolean needDistinctPagination(HyperFilter where) {
        if (!where.hasArrayDescendantNode()) return false;

        for (QResultMapping mapping : this.resultMappings) {
            QJoin join = mapping.getEntityJoin();
            if (join == null) continue;

            if (mapping.getSelectedColumns().size() == 0) continue;

            if (mapping.isArrayNode()) {
                return true;
            }
        }
        return false;
    }

    public String createSelectQuery(JdbcQuery query) {
        sw.reset();
        this.resultMappings = query.getResultMappings();
        HyperFilter where = query.getFilter();

        String tableName = isNativeQuery ? where.getTableExpression(query.getViewParams()) : where.getSchema().getEntityType().getName();
        String select_cmd = (isNativeQuery && !query.isDistinct()) ? "SELECT" : "SELECT DISTINCT";
        boolean need_complex_pagination = isNativeQuery && query.getLimit() > 0 && needDistinctPagination(where);
        if (need_complex_pagination) {
            sw.write("\nWITH _cte AS (\n"); // WITH _cte AS NOT MATERIALIZED
            sw.incTab();
            sw.write(select_cmd).write(" t_0.* ");
            writeFrom(where, tableName, true);
            writeWhere(where);
            tableName = "_cte";
            writeOrderBy(query, false);
            writePagination(query);
            sw.decTab();
            sw.write("\n)");
        }

        sw.writeln("").writeln(select_cmd);
        if (!isNativeQuery) {
            sw.write(where.getMappingAlias()).write(',');
        }
        else {
            for (QResultMapping mapping : this.resultMappings) {
                sw.write('\t');
                String alias = mapping.getMappingAlias();
                for (QColumn col : mapping.getSelectedColumns()) {
                    sw.write(alias).write('.').write(col.getPhysicalName()).write(", ");
                }
                sw.write('\n');
            }
        }
        sw.replaceTrailingComma("\n");
        writeFrom(where, tableName, false);
        writeWhere(where);
        writeOrderBy(query, false);//where.hasArrayDescendantNode());
//        if (!need_complex_pagination && isNativeQuery) {
//            writePagination(query);
//        }
        String sql = sw.reset();
        return sql;
    }

    private void writeOrderBy(JdbcQuery query, boolean need_joined_result_set_ordering) {
        HyperFilter where = query.getFilter();
        Sort sort = query.getSort();
        if (!need_joined_result_set_ordering) {
            if (sort == null || sort.isUnsorted()) return;
        }

        sw.write("\nORDER BY ");
        final HashSet<String> explicitSortColumns = new HashSet<>();
        if (sort != null) {
            QSchema schema = where.getSchema();
            String collation = where.getSchema().getStorage().getSortCollation();
            sort.forEach(order -> {
                String p = order.getProperty();
                String qname = where.getMappingAlias() + '.' + resolveColumnName(schema.getColumn(p));
                explicitSortColumns.add(qname);
                sw.write(qname);
                sw.write(collation);
                sw.write(order.isAscending() ? " asc" : " desc").write(", ");
            });
        }
        if (isNativeQuery && need_joined_result_set_ordering) {
            for (QResultMapping mapping : this.resultMappings) {
                if (!mapping.hasArrayDescendantNode()) continue;
                if (mapping != where && !mapping.isArrayNode()) continue;
                String table = mapping.getMappingAlias();
                for (QColumn column : mapping.getSchema().getPKColumns()) {
                    String qname = table + '.' + column.getPhysicalName();
                    if (!explicitSortColumns.contains(qname)) {
                        sw.write(table).write('.').write(column.getPhysicalName()).write(", ");
                    }
                }
            }
        }
        sw.replaceTrailingComma("");
    }

    private String resolveColumnName(QColumn column) {
        return isNativeQuery ? column.getPhysicalName() : column.getJsonKey();
    }
    private void writePagination(HyperQuery pagination) {
        int offset = pagination.getOffset();
        int limit  = pagination.getLimit();
        // 참고) MariaDB/Mysql 의 경우, Offset 은 Limit 의 하위 statement 이다.
        if (limit > 0) sw.write("\nLIMIT " + limit);
        if (offset > 0) sw.write("\nOFFSET " + offset);
    }

    protected void writeUpdateValueSet(QSchema schema, Map<String, Object> updateSet) {
        writeUpdateValueMap(schema, updateSet, "");
        sw.replaceTrailingComma("\n");
    }

    private void writeUpdateValueMap(QSchema schema, Map<String, Object> updateMap, String base_key) {
        for (Map.Entry<String, Object> entry : updateMap.entrySet()) {
            String key = entry.getKey();
            Object rawValue = entry.getValue();
            if (!USE_FLAT_KEY && rawValue instanceof Map) {
                QColumn col = schema.findColumn(key);
                if (col == null || !col.isJsonNode()) {
                    writeUpdateValueMap(schema, (Map<String, Object>) rawValue, key + ".");
                    continue;
                }
                try {
                    rawValue = objectMapper.writeValueAsString(rawValue);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            QColumn col = schema.getColumn(base_key + key);
            Object value = BatchUpsert.convertJsonValueToColumnValue(col, rawValue);
            sw.write("  ");
            sw.write(col.getPhysicalName()).write(" = ").writeValue(value);
            sw.write(",\n");
        }
        sw.replaceTrailingComma("\n");
    }


    public String createUpdateQuery(HyperFilter where, Map<String, Object> updateSet) {
        sw.write("\nUPDATE ").write(where.getSchema().getTableName()).write(" ").write(where.getMappingAlias()).writeln(" SET");
        writeUpdateValueSet(where.getSchema(), updateSet);
        this.writeWhere(where);
        String sql = sw.reset();
        return sql;

    }

    public String createDeleteQuery(HyperFilter where) {
        sw.write("\nDELETE ");
        this.resultMappings = Collections.emptyList();
        this.noAliases = true;
        this.writeFrom(where, where.getSchema().getTableName(), false);
        this.writeWhere(where);
        String sql = sw.reset();
        return sql;
    }

    public String prepareFindByIdStatement(QSchema schema) {
        sw.write("\nSELECT * FROM ").write(schema.getTableName()).write("\nWHERE ");
        List<QColumn> keys = schema.getPKColumns();
        for (int i = 0; i < keys.size(); ) {
            String key = keys.get(i).getPhysicalName();
            sw.write(key).write(" = ? ");
            if (++ i < keys.size()) {
                sw.write(" AND ");
            }
        }
        String sql = sw.reset();
        return sql;
    }

    protected void writeInsertStatementInternal(QSchema schema, Map entity) {
        Set<String> keys = ((Map<String, ?>)entity).keySet();
        sw.writeln("(");
        sw.incTab();
        for (String name : keys) {
            sw.write(schema.getColumn(name).getPhysicalName());
            sw.write(", ");
        }
        sw.shrinkLength(2);
        sw.decTab();
        sw.writeln("\n) VALUES (");
        for (String k : keys) {
            QColumn col = schema.getColumn(k);
            Object v = BatchUpsert.convertJsonValueToColumnValue(col, entity.get(k));
            sw.writeValue(v).write(", ");
        }
        sw.replaceTrailingComma(")");
    }

    public abstract String createInsertStatement(JdbcSchema schema, Map<String, Object> entity, EntitySet.InsertPolicy insertPolicy);


    protected void writePreparedInsertStatementValueSet(List<JdbcColumn> columns) {
        sw.writeln("(");
        for (QColumn col : columns) {
            sw.write(col.getPhysicalName()).write(", ");
        }
        sw.replaceTrailingComma("\n) VALUES (");
        for (QColumn column : columns) {
            sw.write("?,");
        }
        sw.replaceTrailingComma(")");
    }

    public abstract String prepareBatchInsertStatement(JdbcSchema schema, List<JdbcColumn> columns, EntitySet.InsertPolicy insertPolicy);
}
