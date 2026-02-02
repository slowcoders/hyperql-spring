package org.slowcoders.hyperquery.impl;

import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.slowcoders.hyperquery.core.*;
import org.slowcoders.hyperquery.util.SqlWriter;
import org.springframework.data.annotation.Transient;
import org.w3c.dom.Node;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

public class SqlBuilder extends ViewNode {
    private final HModel rootSchema;
    private final ViewResolver viewResolver;
    private ViewNode currView = this;
    private JoinNode currNode;

    private SqlWriter sbWith = new SqlWriter().write("WITH ");
    private SqlWriter sbQuery = new SqlWriter();

    private XPathParser xpathParser;

    private final Node rootNode;
    private final XNode rootSqlNode;

    private static final String emptyXml = """
    <?xml version="1.0" encoding="UTF-8" ?>
    <sql/>""";

    static String ss = """
                <!DOCTYPE mapper
            PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
            "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

            """;

    public SqlBuilder(HModel schema, ViewResolver viewResolver ) {
        this.rootSchema = schema;
        this.viewResolver = viewResolver;
        this.currNode = new JoinNode(rootSchema, "t_0");
        this.xpathParser = new XPathParser(emptyXml);
        this.rootSqlNode = xpathParser.evalNode("/sql");
        this.rootNode = rootSqlNode.getNode();
    }

    public final HModel getRootSchema() {
        return rootSchema;
    }

    public <R extends QRecord<E>, E extends QEntity<E>> HQuery buildSelect(Class<R> resultType, QFilter<E> filter) {

        List<ColumnMapping> columnMappings = parseColumnMappings(rootSchema, resultType, "");
        HCriteria criteria = HCriteria.parse(this, filter, "@");

        String where = criteria.toString();

        genTableView("t_0", currNode);
        genSelections(columnMappings);
        String baseTable = currNode.views.isEmpty() ? rootSchema.getTableName() : "";
        sbQuery.write("from ").write(baseTable).write(" t_0").write('\n');
        genJoin(currView.joins);

        boolean needMapper = rootNode.hasChildNodes();
        if (sbWith.length() > 5 || needMapper) {
            sbWith.shrinkLength(2);
            sbWith.writeln();
            if (needMapper) {
                sbWith.write("${__sql__}");
                addTextNode(sbWith.reset());
            } else {
                sbWith.write(sbQuery.reset());
                sbQuery = sbWith;
            }
        }

        sbQuery.write("WHERE ").write(where);
        String sql = sbQuery.toString();
//        return sql;
        return new HQuery(needMapper ? rootSqlNode : null, sql);
    }

    void addTextNode(String s) {
        Node text = rootNode.getOwnerDocument().createTextNode(s);
        rootNode.appendChild(text);
    }


    JoinNode pushNamespace(String alias) {
        String aliasQualifier = currNode.aliasQualifier + alias.replaceAll("@", "\\$");
        JoinNode node = currView.getJoin(aliasQualifier);
        if (node == null) {
            QJoin join = viewResolver.getJoin(currNode.getModel(), alias);
            node = new JoinNode(viewResolver.getTargetSchema(join), aliasQualifier);
            currView.addJoin(aliasQualifier, node);
            int orgLevel = node.setAttrLevel(0);
            node.joinCriteria = PredicateTranslator.translate(this, alias, join.getJoinCriteria());
            node.setAttrLevel(orgLevel);
        }
        JoinNode old = currNode;
        this.currNode = node;
        return old;
    }

    void setNamespace(JoinNode node) {
        this.currNode = node;
    }

    public HModel getCurrentModel() {
        return currNode.getModel();
    }

    private interface IdentifierHandler<T> {
        T handleIdentifier(HModel model, String name);
    }

    String resolveLambda(String path, List<String> callArgs) {
        return this.resolveQualifiedAlias(path, (model, name) -> {
            String expr = model.getLambda(name).inflateStatement(this, callArgs);
            return expr;
        });
    }


    public String resolveProperty(String path) {
        return this.resolveQualifiedAlias(path, (model, name) -> {
            QAttribute attr = model.getAttribute(name);
            if (attr != null) {
                ViewNode oldView = currView;
                currView = currNode.pushAttrLevel();
                String expr = attr.inflateStatement(this, "");
                currNode.popAttrLevel();
                currView = oldView;
                if (!currNode.addUsedAttribute(name, expr)) {
                    return expr;
                }
            }
            return currNode.aliasQualifier + '.' + name;
        });
    }

    private <T> T resolveQualifiedAlias(String path, IdentifierHandler<T> handler) {
        String name;
        int p = path.lastIndexOf('.');
        if (p < 0) {
            p = path.length();
            name = "";
        } else {
            name = path.substring(p+1);
        }

        JoinNode baseNode = currNode;
        HModel model = baseNode.getModel();
        if (p > 1) {
            path = path.substring(0, p);
            int left = 1;
            int right;
            for (; (right = path.indexOf('@', left)) > 0; left = right + 1) {
                String alias = path.substring(left - 1, right);
                model = pushNamespace(alias).getModel();
            }
            String alias = path.substring(left - 1);
            model = pushNamespace(alias).getModel();
        }
        T res = handler.handleIdentifier(model, name);
        this.currNode = baseNode;
        return res;
    }

    private void genSelections(List<ColumnMapping> columnMappings) {
        sbQuery.write("SELECT ");
        sbQuery.incTab();
        for (ColumnMapping col : columnMappings) {
            sbQuery.write(col.qualifiedColumnName).write(" as \"").write(col.fieldName).write("\",\n");
        }
        sbQuery.shrinkLength(2);
        sbQuery.decTab();
        sbQuery.write('\n');
    }


    private void genJoin(Map<String, JoinNode> joinNodes) {
        for (Map.Entry<String, JoinNode> entry : joinNodes.entrySet()) {
            String alias = entry.getKey();
            JoinNode node = entry.getValue();
            String tableName = genTableView(alias, node);
            sbQuery.write("left join ").write(tableName).write(" ").write(alias);
            sbQuery.write("\n on ").write(node.joinCriteria).write('\n');
        }
    }

    private String genTableView(String alias, JoinNode node) {
        String tableName = node.getModel().getTableName();
        if (tableName.isEmpty()) {
            sbWith.write(alias).write(" AS (\n");
            sbWith.incTab();
            Object expr = node.getModel().getTableExpression(viewResolver);
            if (expr instanceof Node) {
                if (sbWith.length() > 0) {
                    addTextNode(sbWith.reset());
                }
                Node child = ((Node)expr).getFirstChild();
                for (; child != null; child = child.getNextSibling()) {
                    Node newChild = rootNode.getOwnerDocument().importNode(child, true);
                    rootNode.appendChild(newChild);
                }
            } else {
                sbWith.write(expr.toString());
            }
            sbWith.decTab();
            sbWith.write("), ");
            return tableName;
        }

        if (node.views.isEmpty()) return tableName;

        for (int idx = node.views.size(); --idx >= 0;) {
            ViewNode attrMap = node.views.get(idx);
            sbWith.write(alias);
            if (idx > 0) sbWith.write('_').write(idx);
            sbWith.write(" AS (\n");
            sbWith.write("SELECT ").write(alias).write(".*");
            sbWith.incTab();
            for (Map.Entry<String, String> attr : attrMap.usedAttributes.entrySet()) {
                String name = attr.getKey();
                String expr = attr.getValue();
                sbWith.write("\n, ").write(expr).write(" as ").write(name);
            }
            sbWith.decTab();
            sbWith.write("\nFROM ").write(tableName);
            sbWith.write(" AS ").write(alias).write("\n");
            genJoin(attrMap.joins);
            sbWith.write("\n), ");
            tableName = alias + '_' + (idx);
        }
        return "";
    }

    static Pattern ColumnNameOnly = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
    List<ColumnMapping> parseColumnMappings(HModel view, Class<?> recordType, String propertyPrefix) {
        try {
            List<ColumnMapping> columnMappings = new ArrayList<>();
            for (Field f : recordType.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) ||
                        HSchema.Helper.isTransient(f)) continue;

                String columnExpr = view.getColumnExpr(f);
                if (columnExpr == null) {
                    throw new RuntimeException("Cannot find column expression for " + f.getName());
                }
                Class<? extends QRecord<?>> elementType = HSchema.Helper.getElementType(f);
                if (QRecord.class.isAssignableFrom(elementType)) {
                    JoinNode node = pushNamespace(columnExpr);
                    HSchema subSchema = viewResolver.loadSchema(elementType, false);
                    parseColumnMappings(subSchema, elementType, propertyPrefix + f.getName() + '.');
                    setNamespace(node);
                } else {
                    if (ColumnNameOnly.matcher(columnExpr).matches()) {
                        columnExpr = resolveProperty("@." + columnExpr);
                    } else {
                        columnExpr = PredicateTranslator.translate(this, f.getName(), columnExpr);
                    }
                    columnMappings.add(new ColumnMapping(columnExpr, propertyPrefix, f));
                }
            }
            return columnMappings;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String buildInsert(QUniqueRecord<?> entity, boolean updateOnConflict) {
        List<ColumnMapping> columnMappings = parseColumnMappings(rootSchema, entity.getClass(), "");
        sbQuery.write("INSERT INTO ").write(rootSchema.getTableName()).write(" (");
        for (ColumnMapping mapping : columnMappings) {
            sbQuery.write(mapping.columnName).write(", ");
        }
        sbQuery.shrinkLength(2);
        sbQuery.write(") VALUES (");
        for (ColumnMapping mapping : columnMappings) {
            sbQuery.write("#{").write(mapping.fieldName).write("}, ");
        }
        sbQuery.shrinkLength(2);
        sbQuery.write(")");
        if (updateOnConflict) {
            sbQuery.write("ON CONFLICT (");
            for (ColumnMapping mapping : columnMappings) {
                if (mapping.columnName.equals("id")) {
                    sbQuery.write(mapping.columnName).write(", ");
                }
            }
            sbQuery.replaceTrailingComma(")\nDO UPDATE SET\n");
            for (ColumnMapping mapping : columnMappings) {
                if (mapping.columnName.equals("id")) continue;
                sbQuery.write(mapping.columnName).write(" = #{").write(mapping.fieldName).write("}, ");
            }
            sbQuery.shrinkLength(2);
        }

        return sbQuery.toString();
    }

    public String buildUpdate(QUniqueRecord<?> entity) {
        List<ColumnMapping> columnMappings = parseColumnMappings(rootSchema, entity.getClass(), "");
        sbQuery.write("WITH _DATA as (\n");
        sbQuery.incTab();
        sbQuery.write("select ");
        sbQuery.incTab();
        for (ColumnMapping mapping : columnMappings) {
            sbQuery.write("#{").write(mapping.fieldName).write("} as ").write(mapping.columnName).write(",\n");
        }
        sbQuery.decTab();
        sbQuery.shrinkLength(2);
        sbQuery.decTab();
        sbQuery.write("\n), _OLD as (\n");
        sbQuery.incTab();
        sbQuery.write("select * from ").write(rootSchema.getTableName()).write(" _OLD\n");
        sbQuery.write("JOIN _DATA\n  on ");
        sbQuery.incTab();
        for (ColumnMapping mapping : columnMappings) {
            if (mapping.columnName.equals("id")) {
                sbQuery.write("_OLD.").write(mapping.columnName).write(" = _DATA.").write(mapping.columnName).write("\n AND ");
            }
        }
        sbQuery.shrinkLength(6);
        sbQuery.decTab();
        sbQuery.decTab();
        sbQuery.write("\n), _NEW as (\n");
        sbQuery.incTab();
        sbQuery.write("UPDATE ").write(rootSchema.getTableName()).write(" t_0 SET\n");
        sbQuery.incTab();
        for (ColumnMapping mapping : columnMappings) {
            if (mapping.columnName.equals("id")) continue;
            String value = mapping.columnConfig == null ? "_DATA." + mapping.columnName :
                    mapping.columnConfig.writeTransform().replaceAll("\\?", "_DATA." + mapping.columnName);
            value = value.replaceAll("@", "t_0");
            sbQuery.write(mapping.columnName).write(" = ").write(value).write(",\n");
        }
        sbQuery.shrinkLength(2);
        sbQuery.decTab();
        sbQuery.write("\nFROM _DATA\n");
        sbQuery.write("WHERE ");
        sbQuery.incTab();
        for (ColumnMapping mapping : columnMappings) {
            if (mapping.columnName.equals("id")) {
                sbQuery.write("_DATA.").write(mapping.columnName).write(" = ").write("t_0.").write(mapping.columnName).write("\n AND");
            }
        }
        sbQuery.decTab();
        sbQuery.shrinkLength(5);
        sbQuery.write("\nRETURNING *\n");
        sbQuery.decTab();
        sbQuery.write(")");
        sbQuery.write("select count(*) from _DATA");
        return sbQuery.toString();
    }

    public <E extends QEntity<E>> String buildUpdateCascaded(Object baseId, QJoin join, List<E> subEntities) {
        List<ColumnMapping> columnMappings = parseColumnMappings(rootSchema, rootSchema.getEntityType(), "");
        sbQuery.write("WITH _DATA as (\n");
        sbQuery.incTab();
        if (subEntities.isEmpty()) {
            sbQuery.write("select * from ").write(rootSchema.getTableName()).write(" where false");
        } else {
            sbQuery.incTab();
            sbQuery.write("select ");
            sbQuery.incTab();
            for (ColumnMapping mapping : columnMappings) {
                sbQuery.write("#{data[0].").write(mapping.fieldName).write("} as ").write(mapping.columnName).write(",\n");
            }
            sbQuery.shrinkLength(2);
            sbQuery.decTab();

            for (int i = 1; i < subEntities.size(); i++) {
                sbQuery.write("\nunion all\n");
                sbQuery.write("select ");
                sbQuery.incTab();
                for (ColumnMapping mapping : columnMappings) {
                    sbQuery.write("#{data[").write(i).write("].").write(mapping.fieldName).write("} as ").write(mapping.columnName).write(",\n");
                }
                sbQuery.shrinkLength(2);
                sbQuery.decTab();
            }
            sbQuery.decTab();
        }
        sbQuery.decTab();
        sbQuery.decTab();
        sbQuery.write("\n)");
        sbQuery.write("\nMERGE INTO ").write(rootSchema.getTableName()).write(" as t_0\n");
        sbQuery.write("USING _DATA\n");
        sbQuery.write("ON ");
        for (String pk : rootSchema.getPrimaryKeys()) {
            sbQuery.write("t_0.").write(pk).write(" = _DATA.").write(pk).write(" AND ");
        }
//        sbQuery.write(join.getJoinCriteria());
        sbQuery.shrinkLength(5);
        sbQuery.decTab();
        sbQuery.write("\nWHEN MATCHED THEN\n");
        sbQuery.incTab();
        sbQuery.write("UPDATE SET\n");
        sbQuery.incTab();
        for (ColumnMapping mapping : columnMappings) {
            if (mapping.columnName.equals("id")) continue;
            String value = mapping.columnConfig == null ? "_DATA." + mapping.columnName :
                    mapping.columnConfig.writeTransform().replaceAll("\\?", "_DATA." + mapping.columnName);
            value = value.replaceAll("@", "t_0");
            sbQuery.write(mapping.columnName).write(" = ").write(value).write(",\n");
        }
        sbQuery.shrinkLength(2);
        sbQuery.decTab();
        sbQuery.decTab();
        sbQuery.write("\nWHEN NOT MATCHED THEN\n");
        sbQuery.incTab();
        sbQuery.write("INSERT ").write(" (");
        for (ColumnMapping mapping : columnMappings) {
            sbQuery.write(mapping.columnName).write(", ");
        }
        sbQuery.shrinkLength(2);
        sbQuery.write(") VALUES (");
        for (ColumnMapping mapping : columnMappings) {
            sbQuery.write("_DATA.").write(mapping.columnName).write(", ");
        }
        sbQuery.shrinkLength(2);
        sbQuery.write(")");
        sbQuery.decTab();
        sbQuery.write("\nWHEN NOT MATCHED BY SOURCE\n");
        sbQuery.incTab();
        sbQuery.write("AND EXISTS (\n");
        sbQuery.incTab();
        sbQuery.write("SELECT 1\n");
        sbQuery.write("FROM ").write(join.getSchema().getTableName()).write(" c\n");
        String joinOn = join.getJoinCriteria();
        joinOn = joinOn.replaceAll("#", "t_0");
        joinOn = joinOn.replaceAll("@", "c");
        sbQuery.write("WHERE ").write(joinOn);
        sbQuery.write("    AND c.id = ").write("#{parent.id}");
        sbQuery.decTab().write("\n)");

        sbQuery.decTab();
        sbQuery.write("\nTHEN DELETE;\n");
        sbQuery.write("select count(*) from _DATA");
        return sbQuery.toString();
    }

    public <E extends QEntity<E>> String buildUpdateCascaded2(Object baseId, QJoin join, List<E> subEntities) {
        List<ColumnMapping> columnMappings = parseColumnMappings(rootSchema, rootSchema.getEntityType(), "");
        sbQuery.write("WITH _DATA as (\n");
        sbQuery.incTab();
        if (subEntities.isEmpty()) {
            sbQuery.write("select * from ").write(rootSchema.getTableName()).write(" where false");
        } else {
            sbQuery.incTab();
            sbQuery.write("select ");
            sbQuery.incTab();
            for (ColumnMapping mapping : columnMappings) {
                sbQuery.write("#{data[0].").write(mapping.fieldName).write("}::").write(rootSchema.getColumnType(mapping.columnName)).write(" as ").write(mapping.columnName).write(",\n");
            }
            sbQuery.shrinkLength(2);
            sbQuery.decTab();

            for (int i = 1; i < subEntities.size(); i++) {
                sbQuery.write("\nunion all\n");
                sbQuery.write("select ");
                sbQuery.incTab();
                for (ColumnMapping mapping : columnMappings) {
                    sbQuery.write("#{data[").write(i).write("].").write(mapping.fieldName).write("}::").write(rootSchema.getColumnType(mapping.columnName)).write(" as ").write(mapping.columnName).write(",\n");
                }
                sbQuery.shrinkLength(2);
                sbQuery.decTab();
            }
            sbQuery.decTab();
        }
        sbQuery.decTab();
        sbQuery.decTab();
        sbQuery.write("\n), _UPSERT as (\n");
        sbQuery.incTab();
        sbQuery.write("INSERT INTO ").write(rootSchema.getTableName()).write(" (\n");
        sbQuery.incTab();
        for (ColumnMapping mapping : columnMappings) {
            sbQuery.write(mapping.columnName).write(", ");
        }
        sbQuery.shrinkLength(2);
        sbQuery.decTab();
        sbQuery.write("\n) SELECT \n");
        sbQuery.incTab();
        for (ColumnMapping mapping : columnMappings) {
            sbQuery.write("_DATA.").write(mapping.columnName).write(", ");
        }
        sbQuery.shrinkLength(2);
        sbQuery.decTab();
        sbQuery.write("\nFROM _DATA\n");
        sbQuery.write("ON CONFLICT (");
        for (String col : rootSchema.getPrimaryKeys()) {
            sbQuery.write(col).write(", ");
        }
        sbQuery.shrinkLength(2);
        sbQuery.replaceTrailingComma(")\nDO UPDATE SET\n");
        for (ColumnMapping mapping : columnMappings) {
            if (mapping.columnName.equals("id")) continue;
            String value = mapping.columnConfig == null ? "EXCLUDED." + mapping.columnName :
                    mapping.columnConfig.writeTransform().replaceAll("\\?", "_DATA." + mapping.columnName);
            value = value.replaceAll("@", "t_0");
            sbQuery.write(mapping.columnName).write(" = ").write(value).write(",\n");
        }
        sbQuery.shrinkLength(2);
        sbQuery.decTab();
        sbQuery.decTab();
        sbQuery.write("\n)\nDELETE FROM hql_demo.bookstore.book_order t_0\n");
        sbQuery.incTab();
        sbQuery.write("WHERE NOT EXISTS (\n");
        sbQuery.incTab();
        sbQuery.write("SELECT 1\n");
        sbQuery.write("FROM _DATA\n");
        sbQuery.write("WHERE ");
        sbQuery.incTab();
        for (String col : rootSchema.getPrimaryKeys()) {
            sbQuery.write("t_0.").write(col).write(" = _DATA.").write(col).write("\n AND ");
        }
        sbQuery.decTab();
        sbQuery.shrinkLength(5);
        sbQuery.decTab().write("\n)");
        String joinOn = join.getJoinCriteria();
        joinOn = joinOn.replaceAll("#", "t_0");
        joinOn = joinOn.replaceAll("@\\.(\\w+)", "#{parent.$1}");
        sbQuery.write("AND ").write(joinOn);

        sbQuery.decTab();
        sbQuery.write(";\nselect * from ").write(rootSchema.getTableName());
        sbQuery.write("\nwhere ").write(joinOn);
        return sbQuery.toString();
    }

    static String s2 = """
    WITH _data(customer_id, book_id) AS (
        SELECT NULL::bigint, NULL::bigint
        UNION ALL
        SELECT NULL::bigint, NULL::bigint
    ),
    upserted AS (
        INSERT INTO hql_demo.bookstore.book_order (customer_id, book_id)
        SELECT d.customer_id, d.book_id
        FROM _data d
        WHERE d.customer_id IS NOT NULL
          AND d.book_id IS NOT NULL
        ON CONFLICT (customer_id, book_id)
        DO UPDATE SET
            customer_id = EXCLUDED.customer_id,
            book_id     = EXCLUDED.book_id
        RETURNING customer_id, book_id
    )
    DELETE FROM hql_demo.bookstore.book_order t
    WHERE NOT EXISTS (
        SELECT 1
        FROM _data d
        WHERE d.customer_id = t.customer_id
          AND d.book_id     = t.book_id
    )
    AND t.book_id = 3003
    );
    """;
}
