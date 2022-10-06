package org.slowcoders.jql.parser;

import org.slowcoders.jql.JqlSchema;

import java.util.ArrayList;
import java.util.Collection;

public class QueryBuilder extends SourceWriter<QueryBuilder> implements JqlVisitor {
    private JqlSchema schema;

    public QueryBuilder(JqlSchema schema) {
        super('\'');
        this.schema = schema;
    }

    public JqlSchema setWorkingSchema(JqlSchema jqlSchema) {
        JqlSchema old = this.schema;
        this.schema = jqlSchema;
        return old;
    }

    @Override
    public void writeColumnNames(Iterable<String> names, boolean withTableName) {
        for (String name : names) {
            writeColumnName(name, withTableName).write(", ");
        }
        shrinkLength(2);
    }

    @Override
    public void visitCompare(QAttribute column, CompareOperator operator, Object value) {
        column.printSQL(this);
        String op = "";
        switch (operator) {
            case EQ:
                if (value == null) {
                    value = " IS NULL";
                } else {
                    op = " = ";
                }
                break;
            case NE:
                if (value == null) {
                    value = " IS NOT NULL";
                } else {
                    op = " != ";
                }
                break;

            case GT:
                op = " > ";
                break;
            case LT:
                op = " < ";
                break;
            case LE:
                op = " >= ";
                break;
            case GE:
                op = " <= ";
                break;

            case LIKE:
                op = " LIKE ";
                break;
            case NOT_LIKE:
                op = " NOT LIKE ";
                break;
        }
        this.write(op).writeValue(value);
    }

    @Override
    public void visitNot(Expression statement) {
        this.write(" NOT (");
        statement.accept(this);
        this.write(")");

    }

    @Override
    public void visitMatchAny(QAttribute key, CompareOperator operator, Collection values) {
        if (operator == CompareOperator.EQ || operator == CompareOperator.NE) {
            key.printSQL(this);
        }
        switch (operator) {
            case NE:
                write("NOT ");
                // no-break;
            case EQ:
                write(" IN(");
                writeValues(values);
                write(")");
                break;

            case NOT_LIKE:
                write("NOT ");
                // no-break;
            case LIKE:
                write("(");
                boolean first = true;
                for (Object v : values) {
                    if (first) {
                        first = false;
                    } else {
                        writeQuoted(" OR ");
                    }
                    key.printSQL(this);
                    write(" LIKE ");
                    writeQuoted(v);
                }

        }
    }

    @Override
    public void visitIsNull(QAttribute key, boolean isNull) {
        key.printSQL(this);
        write(" IS NULL");
    }

    @Override
    public void visitAlwaysTrue() {
        write("true");
    }

    @Override
    public void visitPredicateSet(ArrayList<Predicate> predicates, Conjunction conjunction) {
        write("(");
        boolean first = true;
        for (int i = 0; ++i < predicates.size(); ) {
            if (first) {
                first = false;
            } else {
                write(conjunction.toString());
            }
            Predicate item = predicates.get(i);
            item.accept(this);
        }
        write(")");

    }

    @Override
    public void writeWhere(JqlQuery where, boolean includeTableName) {
        if (includeTableName) {
            where.writeJoinStatement(this);
        }
        if (!where.isEmpty()) {
            writeRaw("\nWHERE ");
            where.accept(this);
        }
    }

    @Override
    public QueryBuilder writeColumnName(String name, boolean withTableName) {
        if (withTableName) {
            writeRaw(schema.getTableName()).write('.');
        }
        writeRaw(name);
        return this;
    }

    @Override
    public QueryBuilder writeColumnName(String name) {
        return writeColumnName(name, true);
    }

    @Override
    public QueryBuilder writeTableName() {
        writeRaw(this.schema.getTableName());
        return this;
    }

    public QueryBuilder writeEquals(String column, Object value) {
        this.write(column).write(" = ").writeValue(value);
        return this;
    }
}
