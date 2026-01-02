package org.slowcoders.hyperquery.impl;

import org.slowcoders.hyperquery.core.QEntity;
import org.slowcoders.hyperquery.core.QFilter;

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;

public class QEntityStore {
    Class<? extends QEntity> entityType;

    ArrayList<ColumnMapping> columnMappings = new ArrayList<>();
    ArrayList<ColumnMapping> pkMappings = new ArrayList<>();

    enum EntityCardinality {
        Single,
        Multiple,
        Unknown
    }
    public QEntityStore(Class<? extends QEntity> entityType, String prefix) {
        this.entityType = entityType;

            try {
                for (Field f : entityType.getDeclaredFields()) {
                    QEntity.PKColumn pk = f.getAnnotation(QEntity.PKColumn.class);
                    QEntity.TColumn column = f.getAnnotation(QEntity.TColumn.class);

                    f.setAccessible(true);

                    if (pk != null) {
                        pkMappings.add(new ColumnMapping(pk.value(), f));
                        columnMappings.add(new ColumnMapping(pk.value(), f));
                    }
                    else if (column != null) {
                        columnMappings.add(new ColumnMapping(column.value(), f));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }

    private static final String updateTemplate = """
    WITH _OLD AS (
        SELECT * FROM {1}
        WHERE {2}
    ), _NEW AS (
        WITH _DATA AS (
            {3}
        )
        UPDATE {0} _NEW SET
            {4}
        FROM _OLD, _DATA
        WHERE {5}
        RETURNING _NEW.*
    )
    """;

    private static final String updateOrInsertTemplate = """
    WITH _DATA AS (
        ${__input_data__}
    ), _OLD AS (
        SELECT * FROM ${__table_name__} AS _OLD
        WHERE {__filter_to_exist__}
    ), _NEW AS (
        INSERT INTO ${__table_name__} (${__insert_columns__}) 
        SELECT * FROM _DATA
        ON CONFLICT (${__primary_keys__}) 
        DO UPDATE SET ${__assign_values__} block_alternate_name = EXCLUDED.block_alternate_name
    )
    """;

    private void genValueMappings(StringBuilder sb, String prefix, String suffix) {
        sb.append("(");
        for (ColumnMapping cm : columnMappings) {
            sb.append(prefix).append(cm.field.getName()).append(suffix);
        }
        sb.setLength(sb.length() - 2);
        sb.append(")\n");
    }

    private String genUpdateData(StringBuilder sb, int entityCount) {
        sb.setLength(0);
        sb.append("SELECT * FROM (VALUES\n");
        if (entityCount > 1) {
            for (int i = 0; i < entityCount; i ++) {
                genValueMappings(sb, "#{_parameter[" + i + "].", "}, ");
            }
        } else {
            genValueMappings(sb, "#{", "}, ");
        }
        sb.append(") AS _DATA (");
        for (ColumnMapping cm : columnMappings) {
            sb.append(cm.columnName).append(", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(")");
        return sb.toString();
    }

    private String genUpdateColumnSet(StringBuilder sb) {
        sb.setLength(0);
        for (ColumnMapping cm : columnMappings) {
            sb.append(cm.columnName).append(" = _DATA.").append(cm.columnName).append(",\n");
        }
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    private String genUpdateWhere(StringBuilder sb, int entityCount) {
        sb.setLength(0);
        sb.append("(");
        for (ColumnMapping cm : pkMappings) {
            if (entityCount > 0) {
                sb.append("_NEW.").append(cm.columnName)
                        .append(" = _DATA.").append(cm.columnName).append("\n AND ");
            } else {
                sb.append("_NEW.").append(cm.columnName)
                        .append(" = _OLD.").append(cm.columnName).append("\n AND ");
            }
        }
        sb.setLength(sb.length() - 6);
        sb.append("\n)");
        return sb.toString();
    }

    public void update(QEntity entity, QFilter filter) {
        String tableName = "hpms.hpbk_block_basic_copy";

        StringBuilder sb = new StringBuilder();
        String values = genUpdateData(sb, 0);
        String updateSet = genUpdateColumnSet(sb);
        String updateCondition = genUpdateWhere(sb, 0);
        String sql = MessageFormat.format(updateTemplate,
                tableName, // {0}
                tableName, // {1}
                filter.toString(), // {2}
                values, // {3}
                updateSet, // {4}
                updateCondition); // {5}
    }


    public void insertOrUpdate(QEntity entity) {
        String tableName = "hpms.hpbk_block_basic_copy";

        StringBuilder sb = new StringBuilder();
        String values = genUpdateData(sb, 1);
        String updateSet = genUpdateColumnSet(sb);
        String updateCondition = genUpdateWhere(sb, 1);
        String insertColumns = getInsertColumnNames(sb, "");
        String insertValues = getInsertColumnNames(sb, "_DATA.");

        String sql = MessageFormat.format(updateOrInsertTemplate,
                tableName, // {0}
                tableName, // {1}
                genWhereOnPkParamMatch(sb, "_OLD."), // {2}
                values, // {3}
                updateSet, // {4}
                updateCondition, // {5}
                insertColumns,
                insertValues);

        System.out.println(sql);
    }

    private Object genWhereOnPkParamMatch(StringBuilder sb, String prefix) {
        sb.setLength(0);
        boolean multi_pk = pkMappings.size() > 1;
        if (multi_pk) sb.append("(");
        for (ColumnMapping cm : pkMappings) {
            sb.append(prefix).append(cm.columnName).append(", ");
        }
        sb.setLength(sb.length() - 2);
        if (multi_pk) sb.append(")");

        sb.append(" in (");
        if (multi_pk) sb.append("(");
        for (ColumnMapping cm : pkMappings) {
            sb.append("_DATA.").append(cm.columnName).append(", ");
        }
        sb.setLength(sb.length() - 2);
        if (multi_pk) sb.append(")");
        sb.append(")");
        return sb.toString();
    }

    private String getInsertColumnNames(StringBuilder sb, String prefix) {
        sb.setLength(0);
        for (ColumnMapping cm : columnMappings) {
            sb.append(prefix).append(cm.columnName).append(", ");
        }
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }


    public void delete(QFilter filter) {
        String tableName = "hpms.hpbk_block_basic_copy";
        String deleteTemplate = """
        WITH _OLD AS (
            SELECT * FROM {1}
            WHERE {2}
        ), _NEW AS (
            WITH _DELETED AS (
                DELETE FROM {0}
                USING _OLD
                WHERE _OLD.BLOCK_ID = DELETED.BLOCK_ID
            )
            SELECT T.* from {3} T, _DELETED WHERE false
        )
        """;

        String sql = MessageFormat.format(deleteTemplate,
                tableName,
                tableName,
                filter.toString());
    }


    static class ColumnMapping {
        String columnName;
        Field field;

        public ColumnMapping(String columnName, Field field) {
            this.columnName = columnName;
            this.field = field;
        }
    }
}

