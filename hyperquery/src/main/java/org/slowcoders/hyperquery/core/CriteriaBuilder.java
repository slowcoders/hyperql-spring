package org.slowcoders.hyperquery.core;

import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Stack;

public class CriteriaBuilder {
    Class<? extends QFilter> clazz;
    String prefix;
    CriteriaBuilder(Class<? extends QFilter> clazz, String prefix) {
        this.clazz = clazz;
        this.prefix = prefix;
    }

    QCriteria build() {
        Stack<QCriteria> prStack = new Stack<>();
        QCriteria criteria = new QCriteria(QCriteria.LogicalOp.AND);

        try {
            for (Field f : clazz.getDeclaredFields()) {
                QFilter.Begin[] beginStack = f.getAnnotationsByType(QFilter.Begin.class);
                QFilter.EndOf[] endStack = f.getAnnotationsByType(QFilter.EndOf.class);
                QFilter.Condition condition = f.getAnnotation(QFilter.Condition.class);
                QFilter.JoinFilter subFilter = f.getAnnotation(QFilter.JoinFilter.class);
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
                Object value = f.get(this);
                if (ObjectUtils.isEmpty(value)) continue;

                if (condition != null) {
                    if (subFilter != null) {
                        throw new IllegalStateException("Invalid @Condition + @EmbedFilter pair on " + f.getName());
                    }
                    String expr = condition.value();
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
                    String expr = new CriteriaBuilder((Class<? extends QFilter>) f.getType(), subFilter.value()).build().toString();
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
}