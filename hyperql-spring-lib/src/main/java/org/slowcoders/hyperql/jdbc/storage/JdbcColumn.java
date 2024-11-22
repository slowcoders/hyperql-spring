package org.slowcoders.hyperql.jdbc.storage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.JsonNode;
import org.slowcoders.hyperql.schema.QColumn;
import org.slowcoders.hyperql.schema.QJoin;
import org.slowcoders.hyperql.schema.QSchema;
import org.slowcoders.hyperql.util.ClassUtils;

import java.lang.reflect.Field;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
public class JdbcColumn extends QColumn {

    private final boolean isReadOnly;
    private final boolean isAutoIncrement;
    private final boolean isNullable;
    private final JdbcSchema schema;
    private boolean isPk;
    private Field field;

    private String fieldName;

    private String comment;
    private ColumnBinder fkBinder;

    //*
    private String colTypeName;

    private int displaySize;
    private int precision;
    private int scale;
    //*/

    public JdbcColumn(JdbcSchema schema, ResultSetMetaData md, int col, ColumnBinder fkBinder, String comment, List<String> primaryKeys) throws SQLException {
        super(md.getColumnName(col), resolveJavaType(md, col));
        this.schema = schema;

        this.isAutoIncrement = md.isAutoIncrement(col);
        this.isReadOnly = md.isReadOnly(col) | this.isAutoIncrement;
        this.isNullable = md.isNullable(col) != ResultSetMetaData.columnNoNulls;
        this.isPk = primaryKeys.contains(this.getPhysicalName()) || (isAutoIncrement && primaryKeys.isEmpty());

        this.fkBinder = fkBinder;
        this.field = null;
        boolean isWritable = md.isWritable(col);
        if (!isWritable) {
            throw new RuntimeException("!isWritable");
        }
        this.colTypeName = md.getColumnTypeName(col);
        this.comment = comment; // comment!= null ? comment : md.getColumnLabel(col);
        this.precision = md.getPrecision(col);
        this.scale = md.getScale(col);
        this.displaySize = md.getColumnDisplaySize(col);
    }

    public JdbcSchema getSchema() { return schema; }

    public boolean isForeignKey() { return fkBinder != null; }

    public Field getMappedOrmField() { return this.field; }

    protected void setMappedOrmField(Field f) {
        this.field = f;
        this.fieldName = f.getName();
    }

    @Override
    public String getJsonKey() {
        if (fieldName == null) {
            fieldName = resolveFieldName();
        }
        return fieldName;
    }

    @Override
    public boolean isReadOnly() {
        return this.isReadOnly;
    }

    @Override
    public boolean isNullable() {
        return this.isNullable;
    }

    @Override
    public boolean isAutoIncrement() {
        return this.isAutoIncrement;
    }

    @Override
    public boolean isPrimaryKey() { return this.isPk; }

    @Override
    public String getComment() {
        return this.comment;
    }

    public String getDBColumnType() {
        return colTypeName;
    }

    public QColumn getJoinedPrimaryColumn() {
        if (this.fkBinder == null) return null;
        return fkBinder.getJoinedColumn();
    }

    private String resolveFieldName() {
        StringBuilder sb = new StringBuilder();
        QColumn col = this;
        // if (false) TODO 2024.0930 check to disable scoped name generation.
        for (QColumn joinedPk; (joinedPk = col.getJoinedPrimaryColumn()) != null; col = joinedPk) {
            String token = QJoin.resolveForeignKeyPropertyName(col);
            sb.append(token).append('.');
        }
        QSchema schema = col.getSchema();
        String name = schema.getStorage().toLogicalAttributeName(schema.getSimpleName(), col.getPhysicalName());
        if (this != col) {
            sb.append(name);
            name = sb.toString();
        }
        return name;
    }


    protected void bindPrimaryKey(ColumnBinder pkBinder) {
        this.fkBinder = pkBinder;
    }

    private static Class resolveJavaType(ResultSetMetaData md, int col) throws SQLException {
        String colTypeName = md.getColumnTypeName(col).toLowerCase();
        int colType = md.getColumnType(col);
        try {
            switch (colTypeName) {
                case "longtext": // <- mariadb 에서 json 을 저장하는 type. 일단 json 으로 분류하여, leafProperty 에서 제외.
                case "json":
                case "jsonb":
                    return JsonNode.class;
                default:
                    String javaClassName = md.getColumnClassName(col);
                    return ClassUtils.getBoxedType(Class.forName(javaClassName));
            }

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public String getColumnTypeName() {
        return colTypeName;
    }

    /*package*/ final void markPrimaryKey() {
        this.isPk = true;
    }

    public void setJoinedPrimaryColumn_unsafe(String jsonKey, String pk_table, String pk_column) {
        JdbcSchema schema1 = this.schema;
        if (this.fkBinder != null) {
            throw new RuntimeException("Column is already joined: " + this);
        }
        if (schema1.findColumn(jsonKey) != null) {
            throw new RuntimeException("Key is already used: " + jsonKey);
        }
        JoinConstraint jc = new JoinConstraint(schema1, "");
        jc.add(this);
        this.fkBinder = new ColumnBinder(schema1.getStorage(), pk_table, pk_column);
        QJoin join = new QJoin(schema1, jc);
        join.setJsonKey_unsafe(jsonKey); // jsonKey = mappingKey
        schema1.getEntityJoinMap().put(jsonKey, join);
    }
}
