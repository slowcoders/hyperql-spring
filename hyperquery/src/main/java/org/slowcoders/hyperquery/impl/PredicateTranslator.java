package org.slowcoders.hyperquery.impl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

class PredicateTranslator extends org.slowcoders.hql.core.antlr.PredicateBaseVisitor<String> {
    StringBuilder sb = new StringBuilder();
    SqlBuilder relation;
    String paramName;

    PredicateTranslator(SqlBuilder relation, String paramName) {
        this.relation = relation;
        this.paramName = paramName;
    }

    public static String translate(SqlBuilder sqlBuilder, String paramName, String predicate) {
        org.slowcoders.hql.core.antlr.PredicateLexer lexer = new org.slowcoders.hql.core.antlr.PredicateLexer(CharStreams.fromString(predicate));
        org.slowcoders.hql.core.antlr.PredicateParser parser = new org.slowcoders.hql.core.antlr.PredicateParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.expr();

        PredicateTranslator rewriter = new PredicateTranslator(sqlBuilder, paramName);
        tree.accept(rewriter);
        return rewriter.sb.toString();
    }


    @Override
    public String visitMacroInvocation(org.slowcoders.hql.core.antlr.PredicateParser.MacroInvocationContext ctx) {
        String property = ctx.property().getText();
        List<String> callArgs = new ArrayList<>();

        StringBuilder old_sb = this.sb;
        this.sb = new StringBuilder();
        for (org.slowcoders.hql.core.antlr.PredicateParser.ExprContext arg : ctx.tuple().expr()) {
            sb.setLength(0);
            arg.accept(this);
            callArgs.add(sb.toString());
        }
        String lambdaCall = relation.resolveLambda(property, callArgs);
        this.sb = old_sb;
        sb.append(lambdaCall);
        return "";
    }

    @Override
    public String visitProperty(org.slowcoders.hql.core.antlr.PredicateParser.PropertyContext ctx) {
        String property = ctx.getText();
        String v = relation.resolveProperty(property);
        sb.append(v);
        return "";
    }

    @Override
    public String visitParameter(org.slowcoders.hql.core.antlr.PredicateParser.ParameterContext ctx) {
        String param = ctx.getText();
        sb.append("#{").append(paramName);
        if (param.length() > 1) {
            sb.append('.');
            sb.append(param.substring(2));
        }
        sb.append('}');
        return param;
    }

    @Override
    public String visitTerminal(TerminalNode node) {
        sb.append(node.getText());
        return node.getText();
    }

    @Override
    public String visitErrorNode(ErrorNode node) {
        sb.append(node.getText());
        return node.getText();
    }
}
