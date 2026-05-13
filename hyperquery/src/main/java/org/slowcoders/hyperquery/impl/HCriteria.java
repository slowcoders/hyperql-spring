package org.slowcoders.hyperquery.impl;

import org.apache.ibatis.reflection.MetaObject;
import org.slowcoders.hyperquery.core.QFilter;
import org.slowcoders.hyperquery.util.SqlWriter;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;

public class HCriteria extends ArrayList<String> {

    QFilter.LogicalOp type;

    HCriteria(QFilter.LogicalOp type) {
        this.type = type;
    }

    static HCriteria parse(SqlBuilder generator, QFilter<?> filter, String s) {
        HCriteria criteria = new HCriteria(QFilter.LogicalOp.AND);
        if (filter == null) return criteria;

        Stack<HCriteria> prStack = new Stack<>();

        try {
            for (Field f : filter.getClass().getDeclaredFields()) {
                QFilter.Begin[] beginStack = f.getAnnotationsByType(QFilter.Begin.class);
                QFilter.EndOf[] endStack = f.getAnnotationsByType(QFilter.EndOf.class);
                QFilter.Predicate predicate = f.getAnnotation(QFilter.Predicate.class);

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
                    criteria = new HCriteria(begin.value());
                }

                f.setAccessible(true);
                Object value = f.get(filter);
                if (ObjectUtils.isEmpty(value)) continue;

                boolean isNestedFilter = QFilter.class.isAssignableFrom(f.getType());
                String predicaton;
                if (predicate != null) {
                    predicaton = predicate.value();
                } else if (isNestedFilter) {
                    predicaton = "@." + f.getName();
                } else {
                    String column = generator.getCurrentModel().getColumnExpr(f);
                    if (column == null) {
                        continue;
                    }
                    predicaton = "@." + column + " = ?";
                }

                if (isNestedFilter) {
                    JoinNode node = generator.pushNamespace(predicaton);
                    String expr = parse(generator, (QFilter<?>) value, predicaton).toString();
                    generator.setNamespace(node);
                    criteria.add(expr);
                } else {
                    String expr = PredicateTranslator.translate(generator, f.getName(), predicaton);
                    if (!isIterable(f)) {
                        expr = expr.replaceAll("\\?", "#{" + f.getName() + "}");
                    } else {
                        // @Condition("(@.room_type_code, 5) in (?{?, ?.hash})")
                        expr = parseArrayCondition(expr, f, value);
                    }
                    criteria.add(expr);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

//        HCondition<?> dynamicFilter = filter.createPredicateBuilder(generator).build();
//        SqlWriter sw = new SqlWriter();
//        dynamicFilter.dump(generator.getViewResolver().newMetaObject(filter), sw);
//        criteria.add(sw.toString());
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
                case '(':
                    endChar = ')';
                    break;
                case '[':
                    endChar = ']';
                    break;
                case '{':
                    endChar = '}';
                    break;
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

    public static class Predicate<T> extends HCondition<T> {
        private final String predication;

        public Predicate(QFilter.Validator<T> validator, String predication) {
            super(validator);
            this.predication = predication;
        }

        @Override
        public void dump(MetaObject obj, SqlWriter sw) {
            sw.write(predication);
        }
    }

    public static class PredicateSet<T> extends HCondition<T> {
        private final QFilter.LogicalOp op;
        protected HCondition<T>[] conditions;
        private boolean mustNotEmpty;


        @SafeVarargs
        public PredicateSet(QFilter.LogicalOp op, HCondition<T>... conditions) {
            super(null);
            this.op = op;
            this.conditions = conditions;
        }

        public PredicateSet<T> mustNotEmpty() {
            this.mustNotEmpty = true;
            return this;
        }


        @Override
        public void dump(MetaObject obj, SqlWriter sw) {
            sw.write('(');
            int start = sw.length();
            String conjunction = ' ' + op.toString() + ' ';
            for (HCondition cond : conditions) {
                int len = sw.length();
                cond.dump(obj, sw);
                if (sw.length() != len) {
                    sw.write('\n').write(op.toString()).write(' ');
                }
            }
            if (sw.length() == start) {
                sw.shrinkLength(1);
            } else {
                sw.shrinkLength(conjunction.length());
                sw.write(')');
            }
        }
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

