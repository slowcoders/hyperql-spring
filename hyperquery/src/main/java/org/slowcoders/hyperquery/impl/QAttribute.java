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
        org.slowcoders.hql.core.antlr.SelectionLexer lexer = new org.slowcoders.hql.core.antlr.SelectionLexer(CharStreams.fromString(sql));
        org.slowcoders.hql.core.antlr.SelectionParser parser = new org.slowcoders.hql.core.antlr.SelectionParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.expr();

        SelectTranslator rewriter = new SelectTranslator(generator, paramName);
        tree.accept(rewriter);
        return rewriter.sb.toString();
    }

    public static QAttribute of(String statement) {
        return new QAttribute(statement);
    }

}
