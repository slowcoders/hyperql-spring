package org.slowcoders.hyperql.parser;

import org.slowcoders.hyperql.schema.QColumn;

import java.util.Collection;

public interface PredicateVisitor {

    void visitPredicate(QColumn column, HqlOp operator, Object value);

    void visitNot(Expression statement);

    void visitMatchAny(QColumn column, HqlOp operator, Collection values);

    void visitCompareArray(QColumn column, HqlOp operator, Collection values);

    void visitCompareNull(QColumn column, HqlOp operator);

    void visitPredicates(Collection<Expression> predicates, Conjunction conjunction);

    void visitAlwaysTrue();
}
