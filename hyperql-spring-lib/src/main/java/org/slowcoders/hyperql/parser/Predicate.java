package org.slowcoders.hyperql.parser;

import org.slowcoders.hyperql.schema.QColumn;

import java.util.Collection;

public interface Predicate extends Expression {

    class MatchAny implements Predicate {
        private final QColumn column;
        private final Collection values;
        private final HqlOp operator;

        MatchAny(QColumn column, HqlOp operator, Collection values) {
            this.column = column;
            this.operator = operator;
            this.values = values;
        }

        @Override
        public void accept(PredicateVisitor sb) {
            sb.visitMatchAny(column, operator, values);
        }
    }

    class CompareArray implements Predicate {
        private final QColumn column;
        private final Collection value;
        private final HqlOp operator;

        public CompareArray(QColumn column, HqlOp operator, Collection value) {
            this.column = column;
            this.value = value;
            this.operator = operator;
        }

        @Override
        public void accept(PredicateVisitor sb) {
            sb.visitCompareArray(column, operator, value);
        }
    }

    class Compare implements Predicate {
        private final QColumn column;
        private final Object value;
        private final HqlOp operator;

        public Compare(QColumn column, HqlOp operator, Object value) {
            this.column = column;
            this.value = value;
            this.operator = operator;
        }

        @Override
        public void accept(PredicateVisitor sb) {
            if (value == null) {
                sb.visitCompareNull(column, operator);
            } else {
                sb.visitPredicate(column, operator, value);
            }
        }
    }


    class Not implements Predicate {
        private Expression statement;

        public Not(Expression statement) {
            this.statement = statement;
        }

        @Override
        public void accept(PredicateVisitor sb) {
            sb.visitNot(statement);
        }
    }

}
