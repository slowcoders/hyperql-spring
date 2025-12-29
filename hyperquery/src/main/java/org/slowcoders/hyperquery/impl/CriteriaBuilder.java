package org.slowcoders.hyperquery.impl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slowcoders.hql.core.antlr.PredicateParser;
import org.slowcoders.hyperquery.core.QFilter;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

public class CriteriaBuilder {
    QFilter<?> filter;
    String prefix;
    CriteriaBuilder(QFilter<?> filter, String prefix) {
        this.filter = filter;
        this.prefix = prefix;
    }

    QCriteria build() {
        Stack<QCriteria> prStack = new Stack<>();
        QCriteria criteria = new QCriteria(QCriteria.LogicalOp.AND);

        try {
            for (Field f : filter.getClass().getDeclaredFields()) {
                QFilter.Begin[] beginStack = f.getAnnotationsByType(QFilter.Begin.class);
                QFilter.EndOf[] endStack = f.getAnnotationsByType(QFilter.EndOf.class);
                QFilter.Condition condition = f.getAnnotation(QFilter.Condition.class);
                QFilter.EmbedFilter subFilter = f.getAnnotation(QFilter.EmbedFilter.class);
//                QFilter.LambdaCondition lambda = f.getAnnotation(QFilter.LambdaCondition.class);

                for (QFilter.EndOf end : endStack) {
                    if (end.value() != criteria.type) {
                        throw new IllegalStateException("Mismatch of @Begin + @EndOf pair on " + f.getName());
                    }
                    String sql = criteria.isEmpty() ? null : criteria.toString();
                    criteria = prStack.pop();
                    if (sql != null) {
                        criteria.add(sql);
                    }
                }

                for (QFilter.Begin begin : beginStack) {
                    prStack.push(criteria);
                    criteria = new QCriteria(begin.value());
                }

                f.setAccessible(true);
                Object value = f.get(filter);
                if (ObjectUtils.isEmpty(value)) continue;

                if (condition != null) {
                    if (subFilter != null) {
                        throw new IllegalStateException("Invalid @Condition + @EmbedFilter pair on " + f.getName());
                    }
                    String expr = condition.value();
                    if (false) {
                        expr = parsePredicate(null, f.getName(), expr);
                    }
                    if (!isIterable(f)) {
                        expr = expr.replaceAll("\\?", "#{" + f.getName() + "}");
                    } else {
                        // @Condition("(@.room_type_code, 5) in (?{?, ?.hash})")
                        expr = parseArrayCondition(expr, f, value);
                    }
                    criteria.add(expr);
                }
                else if (subFilter != null) {
                    if (!QFilter.class.isAssignableFrom(f.getType())) {
                        throw new IllegalStateException("Invalid @EmbedFilter on " + f.getName());
                    }
                    String expr = new CriteriaBuilder((QFilter<?>)value, subFilter.value()).build().toString();
                    criteria.add(expr);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return criteria;
    }

    private String parseArrayCondition(String expr, Field f, Object value) {
        int count = (value instanceof Collection) ? ((Collection<?>) value).size() : ((Object[]) value).length;
        boolean isTuple = expr.contains("?.");
        StringBuilder sb = new StringBuilder();
        if (!isTuple) {
            for (int i = 0; i < count; i++) {
                String item = "#{" + f.getName() + "[" + i + "]}";
                sb.append(item).append(", ");
            }
            sb.setLength(sb.length() - 2);
            return expr.replaceAll("\\?", sb.toString());
        }

        int tupleStart = expr.indexOf('?');
        int tupleEnd = tupleStart + 1;
        char startChar = ' ';
        char endChar = ' ';
        if (expr.length() > tupleEnd) {
            startChar = expr.charAt(tupleEnd);
            switch (startChar) {
                case '(': endChar = ')'; break;
                case '[': endChar = ']'; break;
                case '{': endChar = '}'; break;
            }
            if (endChar == ' ' || (tupleEnd = expr.indexOf(endChar, tupleEnd)) == -1) {
                throw new IllegalStateException("Invalid expression: " + expr);
            }
        }

        String tuple = startChar + "#{" + expr.substring(tupleStart + 2, tupleEnd - 1) + "}" + endChar;
        tuple = tuple.replaceAll("\\s*,\\s*", "}, #{");
        for (int i = 0; i < count; i++) {
            String item = f.getName() + "[" + i + "]";
            String injected = tuple.replaceAll("\\?", item);
            sb.append(injected).append(", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append(expr.substring(tupleEnd));
        return sb.toString();
    }

    private static boolean isIterable(Field f) {
        Class<?> type = f.getType();
        return type.isArray()
                || Collection.class.isAssignableFrom(type);
    }

    public String parsePredicate(HSchema relation, String paramName, String predicate) {
        org.slowcoders.hql.core.antlr.PredicateLexer lexer = new org.slowcoders.hql.core.antlr.PredicateLexer(CharStreams.fromString(predicate));
        org.slowcoders.hql.core.antlr.PredicateParser parser = new org.slowcoders.hql.core.antlr.PredicateParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.expr();

        PredicateRewriter rewriter = new PredicateRewriter(relation, paramName);
        tree.accept(rewriter);
        return rewriter.sb.toString();
    }

    static class PredicateRewriter extends org.slowcoders.hql.core.antlr.PredicateBaseVisitor<String> {
        StringBuilder sb = new StringBuilder();
        HSchema relation;
        String paramName;

        PredicateRewriter(HSchema relation, String paramName) {
            this.relation = relation;
            this.paramName = paramName;
        }


        @Override
        public String visitMacroInvocation(org.slowcoders.hql.core.antlr.PredicateParser.MacroInvocationContext ctx) {
            String property = ctx.property().getText();
            String name = property.substring(property.indexOf('.')+1);
            String[] joinPath = property.substring(0, property.indexOf('.')).split("@");

            QLambda lambda = relation.getLambda(joinPath, name);
            List<PredicateParser.ExprContext> args = ctx.tuple().expr();
            StringBuilder old_sb = this.sb;
            this.sb = new StringBuilder();
            List<String> callArgs = new ArrayList<>();
            for (org.slowcoders.hql.core.antlr.PredicateParser.ExprContext arg : args) {
                sb.setLength(0);
                arg.accept(this);
                callArgs.add(sb.toString());
            }
            String lambdaCall = lambda.inflateStatement(callArgs);
            this.sb = old_sb;
            sb.append(lambdaCall);
            return "";
        }

        @Override
        public String visitProperty(org.slowcoders.hql.core.antlr.PredicateParser.PropertyContext ctx) {
            String property = ctx.getText();
            String v = relation.translateProperty(property);
            sb.append(v);
            return "";
        }

        @Override
        public String visitParameter(org.slowcoders.hql.core.antlr.PredicateParser.ParameterContext ctx) {
            String param = ctx.getText();
            sb.append("#{").append(paramName);
            if (param.length() > 1) {
                sb.append('.');
                sb.append(param.substring(2));
            }
            sb.append('}');
            return param;
        }

        @Override
        public String visitTerminal(TerminalNode node) {
            sb.append(node.getText());
            return node.getText();
        }

        @Override
        public String visitErrorNode(ErrorNode node) {
            sb.append(node.getText());
            return node.getText();
        }
    }
}