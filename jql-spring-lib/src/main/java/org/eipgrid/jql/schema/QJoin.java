package org.eipgrid.jql.schema;

import org.eipgrid.jql.util.AttributeNameConverter;
import org.eipgrid.jql.util.ClassUtils;

import javax.persistence.Entity;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

public class QJoin {
    private final QJoin associateJoin;
    private final String jsonKey;
    private final boolean isUnique;
    private final boolean inverseMapped;
    private final List<QColumn> fkColumns;
    private final QSchema baseSchema;
    private final QSchema joinedSchema;

    public QJoin(QSchema baseSchema, List<QColumn> fkColumns) {
        this(baseSchema, fkColumns, null);
    }

    public QJoin(QSchema baseSchema, List<QColumn> fkColumns, QJoin associatedJoin) {
        this.fkColumns = fkColumns;
        this.baseSchema = baseSchema;
        QSchema fkSchema = fkColumns.get(0).getSchema();
        this.inverseMapped = baseSchema != fkSchema;
        if (inverseMapped) {
            assert(associatedJoin == null || !associatedJoin.inverseMapped);
            this.joinedSchema = fkSchema;
            this.isUnique = (associatedJoin == null || associatedJoin.isUnique) &&
                            fkSchema.isUniqueConstrainedColumnSet(fkColumns);
        } else {
            assert(associatedJoin == null);
            this.joinedSchema = fkColumns.get(0).getJoinedPrimaryColumn().getSchema();
            List<QColumn> pkColumns = fkColumns.stream()
                    .map(col -> col.getJoinedPrimaryColumn())
                    .collect(Collectors.toList());
            this.isUnique = joinedSchema.isUniqueConstrainedColumnSet(pkColumns);
        }
        this.associateJoin = associatedJoin;
        this.jsonKey = associatedJoin != null ? associatedJoin.getJsonKey() : initJsonKey();
    }

    public List<QColumn> getForeignKeyColumns() {
        return fkColumns;
    }

    public QJoin getAssociativeJoin() {
        return associateJoin;
    }

    public boolean isInverseMapped() {
        return this.inverseMapped;
    }

    public boolean isUniqueJoin() {
        return this.isUnique;
    }

    public String getJsonKey() {
        return jsonKey;
    }

    private String initJsonKey() {
        if (this.jsonKey != null) {
            throw new RuntimeException("already initialized");
        }
        QColumn first_fk = fkColumns.get(0);
        if (!inverseMapped && fkColumns.size() == 1) {
            return initJsonKey(first_fk);
        }

        String name;
        if (inverseMapped) {
            if (fkColumns.size() == 1) {
                Class<?> jpaType = baseSchema.getJpaType();
                Class<?> jpaClass = getAssociatedSchema__22().getJpaType();
                if (jpaType.getAnnotation(Entity.class) != null) {
                    for (Field f : ClassUtils.getInstanceFields(jpaType, true)) {
                        Class<?> itemT = ClassUtils.getElementType(f);
                        if (jpaClass == itemT) {
                            // TODO MappedBy 검사 필요.
                            return f.getName();
                        };
                    }
                }
            }
            // column 이 없으므로 타입을 이용하여 이름을 정한다.
            name = first_fk.getSchema().getEntityClassName();
        }
        else {
            name = first_fk.getJoinedPrimaryColumn().getSchema().getEntityClassName();
        }
        return name + '_';
    }

    public static String initJsonKey(QColumn fk) {
        if (fk.getMappedOrmField() != null) {
            return fk.getJsonKey();
        }

        String fk_name = fk.getPhysicalName();
        QColumn joinedPk = fk.getJoinedPrimaryColumn();
        String pk_name = joinedPk.getPhysicalName();
        String jkey;
        if (fk_name.endsWith("_" + pk_name)) {
            jkey = AttributeNameConverter.toCamelCase(fk_name.substring(0, fk_name.length() - pk_name.length() - 1), false);
        } else {
            jkey = joinedPk.getSchema().getSimpleTableName();
        }
        return jkey + "_";
    }

    public QSchema getJoinedSchema() {
        QColumn col = fkColumns.get(0);
        if (!inverseMapped) {
            col = col.getJoinedPrimaryColumn();
        }
        return col.getSchema();
    }

    public QSchema getBaseSchema() {
        return this.baseSchema;
    }

    public boolean isJoinedBySingleKey() {
        return fkColumns.size() == 1;
    }

    public QSchema getAssociatedSchema__22() {
        if (associateJoin != null) {
            return associateJoin.getJoinedSchema();
        } else {
            return this.getJoinedSchema();
        }
    }
}
