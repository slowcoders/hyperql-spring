package org.slowcoders.hyperql.jdbc.postgresql;

import org.slowcoders.hyperql.EntitySet;
import org.slowcoders.hyperql.jdbc.storage.JdbcColumn;
import org.slowcoders.hyperql.jdbc.storage.JdbcSchema;
import org.slowcoders.hyperql.jdbc.storage.SqlConverter;
import org.slowcoders.hyperql.jdbc.storage.SqlGenerator;
import org.slowcoders.hyperql.js.JsType;
import org.slowcoders.hyperql.parser.EntityFilter;
import org.slowcoders.hyperql.parser.HqlOp;
import org.slowcoders.hyperql.schema.QColumn;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PGSqlGenerator extends SqlGenerator {
    public PGSqlGenerator(boolean isNativeQuery) {
        super(isNativeQuery);
    }

    public String createInsertStatement(JdbcSchema schema, Map entity, EntitySet.InsertPolicy insertPolicy) {
        sw.writeln();
        sw.write(getCommand(SqlConverter.Command.Insert)).write(" INTO ").write(schema.getTableName());

        super.writeInsertStatementInternal(schema, entity);

        switch (insertPolicy) {
            case IgnoreOnConflict:
                sw.write("\nON CONFLICT DO NOTHING");
                break;
            case UpdateOnConflict:
                if (!schema.hasGeneratedId()) {
                    sw.write("\nON CONFLICT DO UPDATE SET");
                    super.writeUpdateValueSet(schema, entity);
                }
        }
        String sql = sw.reset();
        return sql;
    }

    protected String toSqlExpression(HqlOp operator) {
        /**
         * '?&' 은 text[]를 인자로 받아 top-level object/array 의 key(array 는 value) 존재 확인용. (json 전용)
         * '@>' 은 json object 및 array 를 key/value 일치 여부 확인용. (json 이 아니면, array 전용)
         *  && 는 두 pg-array 간의 intersect 여부 확인용 (그러나, 현재 json_array 를 pg_array 로 casting 하는 것은 불가)
         */
        return switch (operator) {
            case RE -> " ~ ";
            case NOT_RE -> " !~ ";
            case RE_ignoreCase -> " ~* ";
            case NOT_RE_ignoreCase -> " !~* ";
            case CONTAINS -> " ?& ";
            case INTERSECTS -> " ?| ";
            default -> super.toSqlExpression(operator);
        };

    }

    protected void writePreparedInsertStatementValueSet(List<JdbcColumn> columns) {
        sw.writeln("(");
        for (QColumn col : columns) {
            sw.write(col.getPhysicalName()).write(", ");
        }
        sw.replaceTrailingComma("\n) VALUES (");
        for (JdbcColumn column : columns) {
            String dbType = column.getDBColumnType();
            sw.write("?::").write(dbType).write(", ");
        }
        sw.replaceTrailingComma(")");
    }

    @Override
    public String prepareBatchInsertStatement(JdbcSchema schema, List<JdbcColumn> columns, EntitySet.InsertPolicy insertPolicy) {
        sw.writeln();
        sw.write("INSERT INTO ").write(schema.getTableName());

        this.writePreparedInsertStatementValueSet(columns);

        switch (insertPolicy) {
            case IgnoreOnConflict:
                sw.write("\nON CONFLICT DO NOTHING");
                break;
            case UpdateOnConflict:
                if (!schema.hasGeneratedId()) {
                    sw.write("\nON CONFLICT(");
                    for (QColumn col : schema.getPKColumns()) {
                        sw.write(col.getPhysicalName()).write(", ");
                    }
                    sw.replaceTrailingComma(") DO UPDATE SET\n");
                    for (QColumn col : columns) {
                        if (!col.isPrimaryKey()) {
                            String col_name = col.getPhysicalName();
                            sw.write(col_name).write(" = excluded.").write(col_name).write(",\n");
                        }
                    }
                    sw.replaceTrailingComma(";");
                }
        }
        String sql = sw.reset();
        return sql;
    }

    protected void writeJsonPath(EntityFilter node, QColumn column, JsType valueType) {
        sw.write('(');
        writeJsonPath(node);
        if (valueType == JsType.Text) {
            // -> returns json (or jsonb) and ->> returns text
            sw.write('>');
            valueType = null;
        }
        sw.writeQuoted(column.getJsonKey());
        sw.write(')');
        if (valueType != null) {
            writeTypeCast(valueType);
        }
    }

    @Override
    public void visitCompareArray(QColumn column, HqlOp operator, Collection values) {
        writeQualifiedColumnName(column, values);
        String op = toSqlExpression(operator);
        sw.write(op);
        sw.write("ARRAY [");
        sw.writeValues(values);
        sw.write("]");
    }

    protected void writeTypeCast(JsType vf) {
        switch (vf) {
            case Boolean:
                sw.write("::BOOLEAN");
            case Integer:
            case Float:
                sw.write("::NUMERIC");
                break;
            case Date:
                sw.write("::DATE");
                break;
            case Time:
                sw.write("::TIME");
                break;
            case Timestamp:
                sw.write("::TIMESTAMP");
                break;
            case Text:
                sw.write("::TEXT");
                break;
            case Object:
            case Array:
                sw.write("::JSONB");
                break;
        }
    }

    private void writeJsonPath(EntityFilter node) {
        if (node.isJsonNode()) {
            EntityFilter parent = node.getParentNode();
            writeJsonPath(parent);
            if (parent.isJsonNode()) {
                sw.writeQuoted(node.getMappingAlias());
            } else {
                sw.write(node.getMappingAlias());
            }
            sw.write("->");
        } else {
            sw.write(node.getMappingAlias()).write('.');
        }
    }


}
