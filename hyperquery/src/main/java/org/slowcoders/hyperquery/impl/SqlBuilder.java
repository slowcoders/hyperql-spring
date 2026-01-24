package org.slowcoders.hyperquery.impl;

import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.slowcoders.hyperquery.core.*;
import org.slowcoders.hyperquery.util.SqlWriter;
import org.w3c.dom.Node;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

public class SqlBuilder extends ViewNode {
    private final HModel rootSchema;
    private final List<ColumnMapping> columnMappings = new ArrayList<>();
    private final Class<? extends QRecord<?>> resultType;
    private final QFilter<?> filter;

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

    public <R extends QRecord<E>, E extends QEntity<E>> SqlBuilder(HModel schema, Class<R> resultType, QFilter<E> filter, ViewResolver viewResolver ) {
        this.rootSchema = schema;
        this.resultType = resultType;
        this.filter = filter;
        this.viewResolver = viewResolver;
        this.currNode = new JoinNode(rootSchema, "t_0");
        if (filter != null && HSchema.getSchema(filter.getClass(), false) != this.rootSchema.loadSchema()) {
            throw new IllegalArgumentException("Filter type is not related to result type.");
        }
        this.xpathParser = new XPathParser(emptyXml);
        this.rootSqlNode = xpathParser.evalNode("/sql");
        this.rootNode = rootSqlNode.getNode();
    }

    public final HModel getRootSchema() {
        return rootSchema;
    }

    public HQuery buildSelect() {
        List<ColumnMapping> columnMappings = parseColumnMappings(rootSchema, resultType, "");
        HCriteria criteria = HCriteria.parse(this, filter, "@");

        String where = criteria.toString();

        genTableView("t_0", currNode);
        genSelections();
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


    void addColumnMapping(String columnExpr, String fieldName) {
        this.columnMappings.add(new ColumnMapping(columnExpr, fieldName));
    }

    JoinNode pushNamespace(String alias) {
        String aliasQualifier = currNode.aliasQualifier + alias.replaceAll("@", "\\$");
        JoinNode node = currView.getJoin(aliasQualifier);
        if (node == null) {
            QJoin join = currNode.model.getJoin(alias);
            node = new JoinNode(join.getTargetRelation(), aliasQualifier);
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
        return currNode.model;
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
        HModel model = baseNode.model;
        if (p > 1) {
            path = path.substring(0, p);
            int left = 1;
            int right;
            for (; (right = path.indexOf('@', left)) > 0; left = right + 1) {
                String alias = path.substring(left - 1, right);
                model = pushNamespace(alias).model;
            }
            String alias = path.substring(left - 1);
            model = pushNamespace(alias).model;
        }
        T res = handler.handleIdentifier(model, name);
        this.currNode = baseNode;
        return res;
    }

    private void genSelections() {
        sbQuery.write("SELECT ");
        sbQuery.incTab();
        for (ColumnMapping col : columnMappings) {
            sbQuery.write(col.columnName).write(" as \"").write(col.fieldName).write("\",\n");
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
        String tableName = node.model.getTableName();
        if (tableName.isEmpty()) {
            sbWith.write(alias).write(" AS (\n");
            sbWith.incTab();
            Object expr = node.model.getTableExpression(viewResolver);
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
            for (Field f : recordType.getDeclaredFields()) {
                String columnExpr = HSchema.getColumnExpr(view, f);
                if (columnExpr == null) continue;
                Class<? extends QRecord<?>> elementType = HSchema.Helper.getElementType(f);
                if (QRecord.class.isAssignableFrom(elementType)) {
                    JoinNode node = pushNamespace(columnExpr);
                    HSchema subSchema = HSchema.getSchema(elementType, false);
                    parseColumnMappings(subSchema, elementType, propertyPrefix + f.getName() + '.');
                    setNamespace(node);
                } else {
                    if (ColumnNameOnly.matcher(columnExpr).matches()) {
                        columnExpr = resolveProperty("@." + columnExpr);
                    } else {
                        columnExpr = PredicateTranslator.translate(this, f.getName(), columnExpr);
                    }
                    addColumnMapping(columnExpr, propertyPrefix + f.getName());
                }
            }
            return this.columnMappings;
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

    public String buildUpdate(QUniqueRecord<?> entity, boolean updateOnConflict) {
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
            sbQuery.write(mapping.columnName).write(" = _DATA.").write(mapping.columnName).write(",\n");
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

}
