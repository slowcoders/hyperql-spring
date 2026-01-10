package org.slowcoders.hyperquery.impl;

import org.slowcoders.hyperquery.core.QEntity;
import org.slowcoders.hyperquery.core.QFilter;
import org.slowcoders.hyperquery.core.QJoin;
import org.slowcoders.hyperquery.core.QRecord;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

public class SqlBuilder extends ViewNode {
    private final HSchema rootSchema;
    private final List<ColumnMapping> columnMappings = new ArrayList<>();
    private final Class<? extends QRecord<?>> resultType;
    private final QFilter<?> filter;

    private ViewNode currView = this;
    private JoinNode currNode;

    public <R extends QRecord<E>, E extends QEntity<E>> SqlBuilder(Class<R> resultType, QFilter<E> filter) {
        this.rootSchema = HSchema.getSchema(resultType);
        this.resultType = resultType;
        this.filter = filter;
        this.currNode = new JoinNode(rootSchema, "t_0");
        if (filter != null && HSchema.getSchema(filter.getClass()) != this.rootSchema) {
            throw new IllegalArgumentException("Filter type is not related to result type.");
        }
    }

    public final HSchema getRootSchema() {
        return rootSchema;
    }

    public String build() {
        StringBuilder sbWith = new StringBuilder("WITH ");
        StringBuilder sb = new StringBuilder();

        parseSelect(resultType, "");
        QCriteria criteria = QCriteria.parse(this, filter, "@");

        String where = criteria.toString();
        genTableView(sbWith, "t_0", currNode);
        genSelections(sb);
        genFrom(sb, sbWith);
        if (sbWith.length() > 5) {
            sbWith.setLength(sbWith.length() - 2);
            sbWith.append('\n');
            sbWith.append(sb);
            sb = sbWith;
        }
        sb.append("WHERE ").append(where);
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

    private void genSelections(StringBuilder sb) {
        sb.append("select ");
        for (ColumnMapping col : columnMappings) {
            sb.append(col.columnName).append(" as \"").append(col.fieldName).append("\",\n");
        }
        sb.setLength(sb.length() - 2);
        sb.append('\n');
    }

    private void genFrom(StringBuilder sb, StringBuilder sbWith) {
        HSchema relation = HSchema.getSchema(filter.getClass());
        String baseTable = currNode.views.isEmpty() ? relation.getTableName() : "";
        sb.append("from ").append(baseTable).append(" t_0").append('\n');
        genJoin(sb, sbWith, currView.joins);
    }

    private void genJoin(StringBuilder sb, StringBuilder sbWith, Map<String, JoinNode> joinNodes) {
        for (Map.Entry<String, JoinNode> entry : joinNodes.entrySet()) {
            String alias = entry.getKey();
            JoinNode node = entry.getValue();
            String tableName = genTableView(sbWith, alias, node);
            sb.append("left join ").append(tableName).append(" ").append(alias);
            sb.append("\n on ").append(node.joinCriteria).append('\n');
        }
    }

    private String genTableView(StringBuilder sb, String alias, JoinNode node) {
        String tableName = node.model.getTableName();
        if (tableName.isEmpty()) {
            sb.append(alias).append(" AS (\n");
            sb.append(node.model.getQuery()).append("), ");
            return tableName;
        }

        if (node.views.isEmpty()) return tableName;

        for (int idx = node.views.size(); --idx >= 0;) {
            ViewNode attrMap = node.views.get(idx);
            sb.append(alias);
            if (idx > 0) sb.append('_').append(idx);
            sb.append(" AS (\n");
            sb.append("SELECT ").append(alias).append(".*");
            for (Map.Entry<String, String> attr : attrMap.usedAttributes.entrySet()) {
                String name = attr.getKey();
                String expr = attr.getValue();
                sb.append("\n, ").append(expr).append(" as ").append(name);
            }
            sb.append("\nFROM ").append(tableName);
            sb.append(" AS ").append(alias).append("\n");
            genJoin(sb, null, attrMap.joins);
            sb.append("\n), ");
            tableName = alias + '_' + (idx);
        }
        return "";
    }

    static Pattern ColumnNameOnly = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
    void parseSelect(Class<? extends QRecord<?>> recordType, String propertyPrefix) {
        try {
            for (Field f : recordType.getDeclaredFields()) {
                String columnExpr = HModel.Helper.getColumnName(f);
                if (columnExpr == null) continue;
                Class<? extends QRecord<?>> elementType = HModel.Helper.getElementType(f);
                if (QRecord.class.isAssignableFrom(elementType)) {
                    JoinNode node = pushNamespace(columnExpr);
                    parseSelect(elementType, propertyPrefix + f.getName() + '.');
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
