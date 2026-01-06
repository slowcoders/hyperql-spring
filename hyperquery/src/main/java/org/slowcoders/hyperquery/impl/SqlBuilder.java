package org.slowcoders.hyperquery.impl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slowcoders.hyperquery.core.QEntity;
import org.slowcoders.hyperquery.core.QFilter;
import org.slowcoders.hyperquery.core.QJoin;
import org.slowcoders.hyperquery.core.QRecord;

import java.lang.reflect.Field;
import java.util.*;

public class SqlBuilder {
    private final HSchema rootSchema;
    private final List<ColumnMapping> columnMappings = new ArrayList<>();
    private final HashMap<String, QJoin> joins = new HashMap<>();
    private final Class<? extends QRecord<?>> resultType;
    private final QFilter<?> filter;
    private ParsingNode currNode;

    static class ParsingNode {
        final ParsingNode prevNode;
        final HModel model;
        final String aliasQualifier;
        ParsingNode(ParsingNode prevNode, HModel model, String aliasQualifier) {
            this.prevNode = prevNode;
            this.model = model;
            this.aliasQualifier = aliasQualifier;
        }
    }

    public <R extends QRecord<E>, E extends QEntity<E>> SqlBuilder(Class<R> resultType, QFilter<E> filter) {
        this.rootSchema = HSchema.getSchema(resultType);
        this.resultType = resultType;
        this.filter = filter;
        this.currNode = new ParsingNode(null, rootSchema, "");
        if (filter != null && HSchema.getSchema(filter.getClass()) != this.rootSchema) {
            throw new IllegalArgumentException("Filter type is not related to result type.");
        }
    }

    public final HSchema getRootSchema() {
        return rootSchema;
    }

    public String build() {
        parseSelect(resultType, "");
        QCriteria criteria = QCriteria.parse(this, filter, "@");

        StringBuilder sbWith = new StringBuilder("WITH ");
        StringBuilder sb = new StringBuilder();

        String where = criteria.toString();
        genSelections(sb);
        genFrom(sb, sbWith);
        if (sbWith.length() > 5) {
            sbWith.append(sb);
            sb = sbWith;
        }
        sb.append("WHERE ").append(where);
        String sql = sb.toString();
        String[] joins__ = joins.keySet().toArray(new String[joins.size()]);
        for (int idxAlias = joins__.length; --idxAlias >= 0; ) {
            String join = joins__[idxAlias];
            String alias = join.substring(join.lastIndexOf('@') + 1);
            sql = sql.replaceAll(join + "\\b", alias + "_" + (idxAlias));
        }
        sql = sql.replaceAll("@(?=\\W)", "t_0");
        return sql;
    }


    void addColumnMapping(String columnExpr, String fieldName) {
        this.columnMappings.add(new ColumnMapping(columnExpr, fieldName));
    }

    HModel pushNamespace(String alias) {
        QJoin join = currNode.model.getJoin(alias);
        String aliasQualifier = currNode.aliasQualifier + alias;
        this.currNode = new ParsingNode(currNode, join.getTargetRelation(), aliasQualifier);
        this.joins.put(currentNamespace(), join);
        return join.getTargetRelation();
    }

    String currentNamespace() {
        return currNode.aliasQualifier;
    }

    void popNamespace() {
        this.currNode = currNode.prevNode;
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
                return attr.inflateStatement(this, "");
            } else if (currNode.prevNode == null) {
                return path;
            } else {
                return currNode.aliasQualifier + '.' + name;
            }
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

        ParsingNode baseNode = currNode;
        HModel model = baseNode.model;
        if (p > 1) {
            path = path.substring(0, p);
            int left = 1;
            int right;
            for (; (right = path.indexOf('@', left)) > 0; left = right + 1) {
                String alias = path.substring(left - 1, right);
                model = pushNamespace(alias);
            }
            String alias = path.substring(left - 1);
            model = pushNamespace(alias);
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
        HSchema relation = HSchema.getSchema(filter.getClass());// filter.getRelation();
        sb.append("from ").append(relation.getTableName()).append(" @").append('\n');
        for (Map.Entry<String, QJoin> entry : joins.entrySet()) {
            String alias = entry.getKey();
            QJoin join = entry.getValue();
            HModel model = join.getTargetRelation();
            String tableName = model.getTableName();
            if (tableName.isEmpty()) {
                sbWith.append(alias).append(" AS (\n");
                sbWith.append(model.getQuery()).append("), ");
            }
            sb.append("left join ").append(tableName).append(" ").append(alias);
            // replace #*. -> "@" + join + "."
            String self = alias.substring(0, alias.lastIndexOf('@'));
            String joinCriteria = resolveQualifiedAlias(self, (model1, name) -> {
                return translateJoinCriteria(self, join.getJoinCriteria());
            });
            joinCriteria = joinCriteria.replace("#", alias);

            sb.append("\n on ").append(joinCriteria).append('\n');
        }
        if (sbWith.length() > 5) {
            sbWith.setLength(sbWith.length() - 2);
        }
    }

    private String translateJoinCriteria(String nestedAlias, String predicate) {
        org.slowcoders.hql.core.antlr.PredicateLexer lexer = new org.slowcoders.hql.core.antlr.PredicateLexer(CharStreams.fromString(predicate));
        org.slowcoders.hql.core.antlr.PredicateParser parser = new org.slowcoders.hql.core.antlr.PredicateParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.expr();

        PredicateTranslator rewriter = new PredicateTranslator(this, nestedAlias);
        tree.accept(rewriter);
        return rewriter.sb.toString();
    }

    void parseSelect(Class<? extends QRecord<?>> recordType, String propertyPrefix) {
        try {
            for (Field f : recordType.getDeclaredFields()) {
                String columnExpr = HModel.Helper.getColumnName(f);
                if (columnExpr == null) continue;
                Class<? extends QRecord<?>> elementType = HModel.getElementType(f);
                if (QRecord.class.isAssignableFrom(elementType)) {
                    pushNamespace(columnExpr);
                    parseSelect(elementType, propertyPrefix + f.getName() + '.');
                    popNamespace();
                } else {
                    String expr = SelectTranslator.translate(this, f.getName(), columnExpr);
                    addColumnMapping(expr, propertyPrefix + f.getName());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
