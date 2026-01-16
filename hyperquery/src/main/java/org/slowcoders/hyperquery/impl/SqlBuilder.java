package org.slowcoders.hyperquery.impl;

import org.slowcoders.hyperquery.core.*;
import org.slowcoders.hyperquery.util.SqlWriter;

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

    public <R extends QRecord<E>, E extends QEntity<E>> SqlBuilder(HModel schema, Class<R> resultType, QFilter<E> filter, ViewResolver viewResolver ) {
        this.rootSchema = schema;
        this.resultType = resultType;
        this.filter = filter;
        this.viewResolver = viewResolver;
        this.currNode = new JoinNode(rootSchema, "t_0");
        if (filter != null && HSchema.getSchema(filter.getClass(), false) != this.rootSchema.loadSchema()) {
            throw new IllegalArgumentException("Filter type is not related to result type.");
        }
    }

    public final HModel getRootSchema() {
        return rootSchema;
    }

    public String build() {
        List<ColumnMapping> columnMappings = parseSelect(rootSchema, resultType, "");
        QCriteria criteria = QCriteria.parse(this, filter, "@");

        String where = criteria.toString();
        SqlWriter sbWith = new SqlWriter().write("WITH ");
        SqlWriter sb = new SqlWriter();

        genTableView(sbWith, "t_0", currNode);
        genSelections(sb);
        String baseTable = currNode.views.isEmpty() ? rootSchema.getTableName() : "";
        sb.write("from ").write(baseTable).write(" t_0").write('\n');
        genJoin(sb, sbWith, currView.joins);
        if (sbWith.length() > 5) {
            sbWith.shrinkLength(2);
            sbWith.writeln();
            sbWith.write(sb.toString());
            sb = sbWith;
        }
        sb.write("WHERE ").write(where);
        String sql = sb.toString();
        return sql;
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

    private void genSelections(SqlWriter sb) {
        sb.write("SELECT ");
        sb.incTab();
        for (ColumnMapping col : columnMappings) {
            sb.write(col.columnName).write(" as \"").write(col.fieldName).write("\",\n");
        }
        sb.shrinkLength(2);
        sb.decTab();
        sb.write('\n');
    }


    private void genJoin(SqlWriter sb, SqlWriter sbWith, Map<String, JoinNode> joinNodes) {
        for (Map.Entry<String, JoinNode> entry : joinNodes.entrySet()) {
            String alias = entry.getKey();
            JoinNode node = entry.getValue();
            String tableName = genTableView(sbWith, alias, node);
            sb.write("left join ").write(tableName).write(" ").write(alias);
            sb.write("\n on ").write(node.joinCriteria).write('\n');
        }
    }

    private String genTableView(SqlWriter sb, String alias, JoinNode node) {
        String tableName = node.model.getTableName();
        if (tableName.isEmpty()) {
            sb.write(alias).write(" AS (\n");
            sb.incTab();
            sb.write(node.model.getTableExpression(viewResolver));
            sb.decTab();
            sb.write("), ");
            return tableName;
        }

        if (node.views.isEmpty()) return tableName;

        for (int idx = node.views.size(); --idx >= 0;) {
            ViewNode attrMap = node.views.get(idx);
            sb.write(alias);
            if (idx > 0) sb.write('_').write(idx);
            sb.write(" AS (\n");
            sb.write("SELECT ").write(alias).write(".*");
            sb.incTab();
            for (Map.Entry<String, String> attr : attrMap.usedAttributes.entrySet()) {
                String name = attr.getKey();
                String expr = attr.getValue();
                sb.write("\n, ").write(expr).write(" as ").write(name);
            }
            sb.decTab();
            sb.write("\nFROM ").write(tableName);
            sb.write(" AS ").write(alias).write("\n");
            genJoin(sb, null, attrMap.joins);
            sb.write("\n), ");
            tableName = alias + '_' + (idx);
        }
        return "";
    }

    static Pattern ColumnNameOnly = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
    List<ColumnMapping> parseSelect(HModel view, Class<? extends QRecord<?>> recordType, String propertyPrefix) {
        try {
            for (Field f : recordType.getDeclaredFields()) {
                String columnExpr = HSchema.getColumnExpr(view, f);
                if (columnExpr == null) continue;
                Class<? extends QRecord<?>> elementType = HModel.Helper.getElementType(f);
                if (QRecord.class.isAssignableFrom(elementType)) {
                    JoinNode node = pushNamespace(columnExpr);
                    HSchema subSchema = HSchema.getSchema(elementType, false);
                    parseSelect(subSchema, elementType, propertyPrefix + f.getName() + '.');
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

}
