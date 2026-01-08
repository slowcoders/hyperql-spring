package org.slowcoders.hyperquery.impl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class QAttribute {

    private final String sql;
    public QAttribute(String sql) {
        this.sql = sql;
    }

    public String getExpression() { return sql; }

    public final String inflateStatement(SqlBuilder generator, String paramName) {
        return PredicateTranslator.translate(generator, paramName, sql);
    }

    public static QAttribute of(String statement) {
        return new QAttribute(statement);
    }

}
