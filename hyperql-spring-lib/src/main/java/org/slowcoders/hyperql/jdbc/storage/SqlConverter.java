package org.slowcoders.hyperql.jdbc.storage;

import org.slowcoders.hyperql.parser.*;
import org.slowcoders.hyperql.schema.QColumn;
import org.slowcoders.hyperql.util.SourceWriter;

import java.util.*;

public abstract class SqlConverter implements PredicateVisitor {
    protected final SourceWriter sw;
    public enum Command {
        Insert,
        Delete,
        Update,
    }

    public SqlConverter(SourceWriter sw) {
        this.sw = sw;
    }


    protected abstract void writeQualifiedColumnName(QColumn column, Object value);

    protected String toSqlExpression(HqlOp operator) {
        switch (operator) {
            case EQ: return " = ";
            case NE: return " != ";
            case GE: return " >= ";
            case GT: return " > ";
            case LE: return " <= ";
            case LT: return " < ";
            case LIKE: return " LIKE ";
            case NOT_LIKE: return " NOT LIKE ";
            default:
                throw new RuntimeException("not implemented operator: " + operator);
        }
    }
    @Override
    public void visitPredicate(QColumn column, HqlOp operator, Object value) {
        assert (value != null);
        writeQualifiedColumnName(column, value);
        String op = toSqlExpression(operator);
        sw.write(op).writeValue(value);
    }

    @Override
    public void visitNot(Expression statement) {
        sw.write(" NOT ");
        statement.accept(this);
    }

    @Override
    public void visitCompareArray(QColumn column, HqlOp operator, Collection values) {
        throw new RuntimeException("Not implemted");
    }

    private Object getElementValue(Object value) {
        if (value == null) return null;

        if (value instanceof Collection) {
            for (Object v : (Collection) value) {
                if (v != null) return v;
            }
            return null;
        }
        if (value instanceof Object[]) {
            for (Object v : (Object[]) value) {
                if (v != null) return v;
            }
            return null;
        }

        return value;
    }

    @Override
    public void visitMatchAny(QColumn column, HqlOp operator, Collection values) {
        boolean is_not = operator == HqlOp.NE || operator == HqlOp.NOT_LIKE;
        boolean contains_null = values.contains(null);
        if (contains_null) {
            ArrayList arrayList = new ArrayList(values);
            for (int i; (i = arrayList.indexOf(null)) >= 0; ) { arrayList.remove(i); };
            values = arrayList;
        }

        if (is_not || contains_null) {
            sw.write("(");
            writeQualifiedColumnName(column, values);
            sw.write(is_not && contains_null ? " IS NOT " : " IS ").write("NULL OR ");
            contains_null = true;
        }

        if (operator == HqlOp.EQ || operator == HqlOp.NE) {
            Object typeValue = getElementValue(values);
            writeQualifiedColumnName(column, typeValue);
        }

        if (values.isEmpty()) {
            sw.write(is_not ? " IS NOT NULL" : " IN (NULL)");
        }
        else {
            switch (operator) {
                case NE:
                    sw.write(" NOT");
                    // no-break;
                case EQ:
                    sw.write(" IN(");
                    sw.writeValues(values);
                    sw.write(")");
                    break;

                case NOT_LIKE:
                    sw.write(" NOT ");
                    // no-break;
                case LIKE:
                    sw.write("(");
                    boolean first = true;
                    for (Object v : values) {
                        if (first) {
                            first = false;
                        } else {
                            sw.write(" OR ");
                        }
                        writeQualifiedColumnName(column, "");
                        sw.write(" LIKE ");
                        sw.writeQuoted(v);
                    }
                    sw.write(")");
                    break;

                default:
                    throw new RuntimeException("Invalid match any operator: " + operator);
            }
        }
        if (contains_null) {
            sw.write(")");
        }
    }

    @Override
    public void visitCompareNull(QColumn column, HqlOp operator) {
        String value;
        switch (operator) {
            case EQ:
                value = " IS NULL";
                break;
            case NE:
                value = " IS NOT NULL";
                break;
            default:
                throw new RuntimeException("Invalid match operator with null value: " + operator);
        }
        writeQualifiedColumnName(column, null);
        //key.printSQL(this);
        sw.write(value);
    }

    @Override
    public void visitAlwaysTrue() {
        sw.write("true");
    }

    @Override
    public void visitPredicates(Collection<Expression> predicates, Conjunction conjunction) {
        sw.write("(");
        boolean first = true;
        for (Expression item : predicates) {
            if (item.isEmpty()) continue;

            if (first) {
                first = false;
            } else {
                sw.write(conjunction.toString());
            }
            item.accept(this);
        }
        sw.write(")");
    }

}
