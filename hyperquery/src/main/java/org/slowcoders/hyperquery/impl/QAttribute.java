package org.slowcoders.hyperquery.impl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class QAttribute {

    private final String sql;
    boolean inProgress = false;
    public QAttribute(String sql) {
        this.sql = sql;
    }

    public String getExpression() { return sql; }

    public synchronized final String inflateStatement(SqlBuilder generator, String paramName) {
        if (inProgress) {
            throw new IllegalStateException("Circular attribute reference is found.");
        }
        try {
            inProgress = true;
            String expr = PredicateTranslator.translate(generator, paramName, sql);
            return expr;
        } finally {
            inProgress = false;
        }
    }

    public static QAttribute of(String statement) {
        return new QAttribute(statement);
    }

}
