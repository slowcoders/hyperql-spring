package org.slowcoders.hyperquery.impl;

import org.slowcoders.hyperquery.core.QFilter;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;

public class QCriteria extends ArrayList<String> {

    public enum LogicalOp {
        AND, OR, NOT_AND, NOT_OR;
    }

    LogicalOp type;

    QCriteria(LogicalOp type) {
        this.type = type;
    }

    static QCriteria parse(SqlBuilder generator, QFilter<?> filter, String s) {
        QCriteria criteria = new QCriteria(QCriteria.LogicalOp.AND);
        if (filter == null) return criteria;

        Stack<QCriteria> prStack = new Stack<>();

        try {
            for (Field f : filter.getClass().getDeclaredFields()) {
                QFilter.Begin[] beginStack = f.getAnnotationsByType(QFilter.Begin.class);
                QFilter.EndOf[] endStack = f.getAnnotationsByType(QFilter.EndOf.class);
                QFilter.Predicate predicate = f.getAnnotation(QFilter.Predicate.class);
                QFilter.EmbedFilter subFilter = f.getAnnotation(QFilter.EmbedFilter.class);

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

                if (predicate != null) {
                    if (subFilter != null) {
                        throw new IllegalStateException("Invalid @Condition + @EmbedFilter pair on " + f.getName());
                    }
                    String expr = PredicateTranslator.translate(generator, f.getName(), predicate.value());
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
                    JoinNode node = generator.pushNamespace(subFilter.value());
                    String expr = parse(generator, (QFilter<?>)value, subFilter.value()).toString();
//                    String expr = new CriteriaBuilder(generator, (QFilter<?>)value, subFilter.value()).build().toString();
                    generator.setNamespace(node);
                    criteria.add(expr);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return criteria;
    }

    private static String parseArrayCondition(String expr, Field f, Object value) {
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


    public String toString() {
        if (this.isEmpty()) return "true";

        String delimiter = "";
        StringBuilder sb = new StringBuilder();
        switch (type) {
            case NOT_AND:
                sb.append("NOT ");
            case AND:
                delimiter = " AND ";
                break;
            case NOT_OR:
                sb.append("NOT ");
            case OR:
                delimiter = " OR ";
        }
        sb.append("(");
        for (String predication : this) {
            sb.append(predication).append(delimiter);
        }
        sb.setLength(sb.length() - delimiter.length());
        sb.append(")");
        return sb.toString();
    }

}

