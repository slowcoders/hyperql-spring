package org.slowcoders.hyperql.jdbc.storage;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.slowcoders.hyperql.HyperRepository;
import org.slowcoders.hyperql.schema.EntityAccessGuard;
import org.slowcoders.hyperql.schema.EntityAccessType;
import org.slowcoders.hyperql.jdbc.JdbcStorage;
import org.slowcoders.hyperql.jpa.JpaUtils;
import org.slowcoders.hyperql.schema.QColumn;
import org.slowcoders.hyperql.schema.QJoin;
import org.slowcoders.hyperql.schema.QSchema;
import org.slowcoders.hyperql.util.SourceWriter;

import jakarta.persistence.IdClass;
import java.lang.reflect.Field;
import java.util.*;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class JdbcSchema extends QSchema {

    private final JdbcStorage schemaLoader;

    private Map<String, ArrayList<String>> uniqueConstraints = new HashMap<>();
    private final HashMap<String, JoinConstraint> fkConstraints = new HashMap<>();

    private Map<String, QJoin> entityJoinMap;
    private ArrayList<QColumn> unresolvedJpaColumns;
    private Class<?> idType;
    private JoinMap importedByFkJoinMap;
    private EntityAccessGuard accessGuard;

    protected JdbcSchema(JdbcStorage schemaLoader, String tableName, Class<?> ormType) {
        super(tableName, ormType);
        this.schemaLoader = schemaLoader;
    }

    public final JdbcStorage getStorage() {
        return schemaLoader;
    }

    public EntityAccessGuard getAccessGuard() {
        return accessGuard;
    }

    public void setAccessGuard(EntityAccessGuard accessGuard) {
        this.accessGuard = accessGuard;
    }

    protected void init(ArrayList<JdbcColumn> columns, Map<String, ArrayList<String>> uniqueConstraints, Class<?> ormType) {
        this.uniqueConstraints = uniqueConstraints;

        if (!HyperRepository.rawEntityType.isAssignableFrom(ormType)) {
            HashMap<String, Field> jpaColumns = new HashMap<>();
            for (Field f: JpaUtils.getColumnFields(ormType)) {
                String name = resolvePhysicalName(f);
                jpaColumns.put(name.toLowerCase(), f);
            }

            for (int i = columns.size(); --i >= 0; ) {
                JdbcColumn col = columns.get(i);
                Field f = jpaColumns.get(col.getPhysicalName().toLowerCase());
                if (f == null) {
                    columns.remove(i);
                    if (unresolvedJpaColumns == null) unresolvedJpaColumns = new ArrayList<>();
                    unresolvedJpaColumns.add(col);
                }
                else {
                    super.mapColumn(col, f);
                }
            }
        }
        super.init(columns, ormType);
        this.importedByFkJoinMap = new JoinMap(this);
        for (JoinConstraint fkColumns : this.fkConstraints.values()) {
            importedByFkJoinMap.addImportedJoin(fkColumns);
        }
    }

    protected void markAllColumnsToPK(List<QColumn> pkColumns) {
        for (QColumn col : pkColumns) {
            ((JdbcColumn)col).markPrimaryKey();
        }
    }

    public final Map<String, QJoin> getEntityJoinMap() {
        return getEntityJoinMap(true);
    }

    protected Map<String, QJoin> getEntityJoinMap(boolean loadNow) {
        if (loadNow && this.entityJoinMap == null) {
            schemaLoader.loadJoinMap(this);
        }
        return this.entityJoinMap;
    }

    protected void setEntityJoinMap(Map<String, QJoin> joinMap) {
        if (this.entityJoinMap != null) {
            throw new RuntimeException("entityJoinMap is already assigned.");
        }
        this.entityJoinMap = joinMap;
    }

    public static void dumpJPAHeader(SourceWriter sb, boolean includeJsonType) {
        if (includeJsonType) {
            sb.writeln("import com.fasterxml.jackson.databind.JsonNode;");
        }
        sb.writeln("import lombok.Getter;");
        sb.writeln("import lombok.Setter;");
        sb.writeln("import java.util.*;");
        sb.writeln("import jakarta.persistence.*;");
        sb.writeln();
        sb.writeln("/** This source is generated by HQL-JDBC */\n");
    }

    public void dumpJPAEntitySchema(SourceWriter sb, boolean includeHeader) {
        if (includeHeader) {
            dumpJPAHeader(sb, !this.getExtendedColumns().isEmpty());
        }

        String comment = this.schemaLoader.getTableComment(this.getTableName());

        if (comment != null && comment.length() > 0) {
            sb.write("/** ").write(comment).writeln(" */");
        }
        sb.writeln("@Entity");
        dumpTableDefinition(sb);

        /* TODO multi-key, EmbeddedId, Embeddable, Element */
        boolean isMultiPKs = false && getPKColumns().size() > 1;
        String className = this.generateEntityClassName();
        if (isMultiPKs) {
            sb.write("@IdClass(").write(className).writeln(".ID.class)");
        }
        sb.writeln("public class " + className + " implements java.io.Serializable {");
        if (isMultiPKs) {
            sb.incTab();
            sb.write("public static class ID implements Serializable {\n");
            sb.incTab();
            for (QColumn column : getPKColumns()) {
                dumpColumnDefinition(column, sb);
            }
            sb.decTab();
            sb.decTab();
        }
        sb.incTab();
        //idColumns = getIDColumns();
        for (QColumn col : getReadableColumns()) {
            dumpColumnDefinition(col, sb);
            sb.writeln();
        }

        for (Map.Entry<String, QJoin> entry : this.getEntityJoinMap().entrySet()) {
            QJoin join = entry.getValue();
            dumpJoinedColumn(join, sb);

        }
        sb.decTab();
        sb.writeln("}\n");
    }

    private void dumpColumnDefinition(QColumn col, SourceWriter sb) {
        if (col.getJoinedPrimaryColumn() != null) return;

        if (col.getComment() != null) {
            sb.write("/** ");
            sb.write(col.getComment());
            sb.writeln(" */");
        }
        boolean isJsonObject = col.isJsonNode();
        if (true || !isJsonObject) {
            sb.write("@Getter");
            if (!col.isReadOnly()) {
                sb.write(" @Setter");
            }
            sb.writeln();
        }

        if (col.isPrimaryKey()) {
            sb.writeln("@Id");
            if (col.isAutoIncrement()) {
                sb.writeln("@GeneratedValue(strategy = GenerationType.IDENTITY)");
            }
        }
        QColumn pk = col.getJoinedPrimaryColumn();
        if (pk != null) {
            boolean isUnique = this.isUniqueConstrainedColumnSet(Collections.singletonList(col));
            sb.write(isUnique ? "@One" : "@Many").writeln("ToOne(fetch = FetchType.LAZY)");
        }
        sb.write(pk != null ? "@Join" : "@").write("Column(name = ").writeQuoted(col.getPhysicalName()).write(", ");
        if (col.isNullable()) sb.write("nullable = true, ");
        if (!col.getValueType().getName().startsWith("java.lang.")) {
            sb.write("columnDefinition = \"").write(((JdbcColumn)col).getColumnTypeName()).write("\", ");
        }
        if (pk != null) {
            sb.write("referencedColumnName = ").writeQuoted(pk.getPhysicalName()).write(", ");
        }
        if (!col.isNullable()) {
            sb.writeln("nullable = false");
        }

        sb.replaceTrailingComma(")\n");

        if (isJsonObject) {
            sb.writeln("@org.hibernate.annotations.Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)");
        }
        String fieldName = getJavaFieldName(col);

//        if (isJsonObject) {
//            sb.write("String ").write(fieldName).writeln(";");
//            fieldName = fieldName + "$";
//            sb.writeln();
//            sb.write("transient ");
//        }
        sb.write("private ").write(getJavaFieldType(col)).write(" ").write(fieldName).writeln(";");

    }

    private void dumpJoinedColumn(QJoin join, SourceWriter sb) {
        boolean isInverseJoin = join.isInverseMapped();
        QColumn firstFk = join.getJoinConstraint().get(0);
        if (isInverseJoin && join.getAssociativeJoin() == null && firstFk.getSchema().hasOnlyForeignKeys()) {
            return;
        }

        QSchema mappedSchema = join.getTargetSchema();
        boolean isUniqueJoin = join.hasUniqueTarget();
        boolean isArrayJoin = isInverseJoin && !isUniqueJoin;

        if (!isInverseJoin && join.getJoinConstraint().size() == 1) {
            QColumn col = firstFk;
            if (join.getAssociativeJoin() != null && join.getAssociativeJoin().getJoinConstraint().size() == 1) {
                col = join.getAssociativeJoin().getJoinConstraint().get(0);
            }
            if (col.getComment() != null) {
                sb.write("/** ");
                sb.write(col.getComment());
                sb.writeln(" */");
            }
        }

        sb.write("@Getter @Setter\n");

        if (!isInverseJoin && firstFk.isPrimaryKey()) {
            sb.write("@Id\n");
        }

        sb.write('@').write(join.getType().toString()).write("(fetch = FetchType.LAZY");
        if (isInverseJoin && join.getAssociativeJoin() == null) {
            String mappedField = getJavaFieldName(firstFk);
            sb.write(", mappedBy = ").writeQuoted(mappedField);
        }
        sb.write(")\n");

        if (!isInverseJoin) {
            QColumn fk = firstFk;
            sb.write("@JoinColumn(name = ").writeQuoted(fk.getPhysicalName()).write(", ");
            if (fk.isNullable()) sb.write("nullable = true, ");
            sb.write("referencedColumnName = ").writeQuoted(fk.getJoinedPrimaryColumn().getPhysicalName());
            sb.incTab();
            var fkConstraints = this.getFKConstraintName(fk);
            if (fkConstraints != null) {
                sb.write(",\nforeignKey = @ForeignKey(name = ").writeQuoted(fkConstraints).write(")");
            }
            sb.write(")\n");
            sb.decTab();
        }
        else if (join.getAssociativeJoin() != null) {
            QSchema associateSchema = join.getLinkedSchema();
            sb.write("@JoinTable(name = ").writeQuoted(associateSchema.getSimpleName()).write(", ");
            String namespace = ((JdbcSchema)associateSchema).getNamespace();
            if (namespace != null) {
                sb.write("schema = ").writeQuoted(namespace).write(", ");
                sb.write("catalog = ").writeQuoted(namespace).write(",");
            }
            sb.writeln();
            sb.incTab();
            if (firstFk.getSchema().hasOnlyForeignKeys()) {
                ((JdbcSchema)firstFk.getSchema()).dumpUniqueConstraints(sb);
            }
            sb.write("joinColumns = @JoinColumn(name=").writeQuoted(firstFk.getPhysicalName()).write("), ");
            sb.write("inverseJoinColumns = @JoinColumn(name=").writeQuoted(join.getAssociativeJoin().getJoinConstraint().get(0).getPhysicalName()).write("))\n");
            sb.decTab();
        }

        String mappedType = ((JdbcSchema)mappedSchema).generateEntityClassName();
        sb.write("private ");
        if (!isArrayJoin) {
            sb.write(mappedType);
        } else {
            JdbcSchema fkSchema = (JdbcSchema)firstFk.getSchema();
            /* List 칼럼 여러 개를 동시에 검색하면 Hibernate 가 MultiBag 오류를 발생시킨다.
               Set 을 사용하지 않으면, 동일 Data 가 중복되는 문제가 발생한다.
               2022.02.17
               현재로선 List 를 반드시 사용해야 하는 경우가 파악되지 않는다. 이제 일단 모든 Array 를 Set 으로 처리.
             */
            boolean partOfUnique = true || fkSchema.hasGeneratedId() || fkSchema.uniqueConstraints.size() > 0;
            sb.write(partOfUnique ? "Set<" : "List<").write(mappedType).write(">");
        }
        sb.write(" ").write(getJavaFieldName(join)).write(";\n\n");
    }

    private String getFKConstraintName(QColumn fk) {
        for (Map.Entry<String, JoinConstraint> constraint : fkConstraints.entrySet()) {
            JoinConstraint cols = constraint.getValue();
            if (cols.contains(fk)) {
                return constraint.getKey();
            }
        }
        return null;
    }

    private String getNamespace() {
        String tableName = this.getTableName();
        int r = tableName.lastIndexOf('.');
        if (r < 0) return null;
        int l = tableName.indexOf('.') + 1;
        if (l >= r) l = 0;
        return tableName.substring(l, r);
    }

    private void dumpTableDefinition(SourceWriter sb) {
        sb.write("@Table(name = ").writeQuoted(this.getSimpleName()).write(", ");
        String namespace = this.getNamespace();
        if (namespace != null) {
            sb.write("schema = ").writeQuoted(namespace).write(", ");
            sb.write("catalog = ").writeQuoted(namespace).write(",");
        }
        sb.writeln();
        sb.incTab();
        dumpUniqueConstraints(sb);
        sb.decTab();
        sb.replaceTrailingComma("\n)\n");
    }

    private void dumpUniqueConstraints(SourceWriter sb) {
        if (!this.uniqueConstraints.isEmpty()) {
            sb.write("uniqueConstraints = {");
            sb.incTab();
            for (Map.Entry<String, ArrayList<String>> entry: this.uniqueConstraints.entrySet()) {
                sb.write("\n@UniqueConstraint(name =\"" + entry.getKey() + "\", columnNames = {");
                sb.incTab();
                for (String column : entry.getValue()) {
                    sb.writeQuoted(column).write(", ");
                }
                sb.replaceTrailingComma("}),");
                sb.decTab();
            }
            sb.decTab();
            sb.replaceTrailingComma("\n},\n");
        }
    }

    private String getJavaFieldName(QJoin join) {
        String name = join.getJsonKey();
        if (name.charAt(0) == '+') {
            name = name.substring(1);
        }
        return name;
    }



    private String getJavaFieldType(QColumn col) {
        String name = col.getValueType().getName();
        if (name.startsWith("java.lang.")) {
            name = name.substring(10);
        }
        return name;
    }


    public boolean isUniqueConstrainedColumnSet(List<QColumn> fkColumns) {
        int cntColumn = fkColumns.size();
        compare_constraint:
        for (List<String> uc : this.uniqueConstraints.values()) {
            if (uc.size() != cntColumn) continue;
            for (QColumn col : fkColumns) {
                String col_name = col.getPhysicalName();
                if (!uc.contains(col_name)) {
                    continue compare_constraint;
                }
            }
            return true;
        }
        return false;
    }

    protected QJoin getJoinByForeignKeyConstraints(String fkConstraint) {
        List<QColumn> fkColumns = this.fkConstraints.get(fkConstraint);
        for (QJoin join : this.importedByFkJoinMap.values()) {
            if (join.getJoinConstraint() == fkColumns) {
                assert(join.getBaseSchema() == this && !join.isInverseMapped());
                return join;
            }
        }
        for (QJoin join : this.importedByFkJoinMap.values()) {
            QSchema schema = join.getTargetSchema();// getForeignKeyColumns().get(0).getSchema();
            System.out.println(schema.getTableName());
        }
        throw new Error("fk join not found: " + fkConstraint);
    }

    HashMap<String, JoinConstraint> getForeignKeyConstraints() {
        return this.fkConstraints;
    }
    protected void addForeignKeyConstraint(String fk_name, JdbcColumn fkColumn) {
        JoinConstraint fkColumns = fkConstraints.get(fk_name);
        if (fkColumns == null) {
            fkColumns = new JoinConstraint(this, fk_name);
            fkColumns.add(fkColumn);
            fkConstraints.put(fk_name, fkColumns);
        } else {
            fkColumns.add(fkColumn);
        }
    }

    private String resolvePhysicalName(Field f) {
        String colName = JpaUtils.getPhysicalColumnNameOrNull(f);
        if (colName == null) {
            colName = schemaLoader.toPhysicalColumnName(f.getName());
        }
        return colName;
    }

    public Class<?> getIdType() {
        if (this.idType == null) {
            List<QColumn> pkColumns = this.getPKColumns();
            if (pkColumns.size() == 1) {
                this.idType = pkColumns.get(0).getValueType();
            }
            else if (!isJPARequired()) {
                this.idType = JdbcArrayID.class;
            }
            else {
                IdClass idClass = getEntityType().getAnnotation(IdClass.class);
                this.idType = idClass.value();
            }
        }
        return this.idType;
    }
    public <ID, ENTITY> ID getEnityId(ENTITY entity) {
        if (entity == null) return null;
        if (!getEntityType().isAssignableFrom(entity.getClass())) {
            throw new RuntimeException("Entity type mismatch: " +
                    getEntityType().getSimpleName() + " != " + entity.getClass().getSimpleName());
        }

        try {
            List<QColumn> pkColumns = this.getPKColumns();
            if (pkColumns.size() == 1) {
                QColumn pk = pkColumns.get(0);
                if (this.isJPARequired()) {
                    Field f = pk.getMappedOrmField();
                    f.setAccessible(true);
                    return (ID)f.get(entity);
                } else {
                    return (ID)((Map)entity).get(pk.getJsonKey());
                }
            }
            else {
                if (this.isJPARequired()) {
                    Object id = getIdType().getConstructor().newInstance();
                    for (QColumn column : pkColumns) {
                        Object k = column.getMappedOrmField().get(entity);
                        column.getMappedOrmField().set(id, k);
                    }
                    return (ID)id;
                }
                else {
                    Object[] keys = new Object[pkColumns.size()];
                    int i = 0;
                    for (QColumn column : pkColumns) {
                        keys[i++] = ((Map)entity).get(column.getJsonKey());
                    }
                    return (ID)new JdbcArrayID(keys);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*package*/ final Map<String, QJoin> getEntityJoinMap_unsafe() {
        return this.entityJoinMap;
    }

    /*packet*/ final JoinMap getImportedJoins() {
        return importedByFkJoinMap;
    }

    public String getSampleQuery() {
        return "select * from " + this.getTableName();
    }

}
