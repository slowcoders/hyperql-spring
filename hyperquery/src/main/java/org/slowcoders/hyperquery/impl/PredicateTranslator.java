package org.slowcoders.hyperquery.impl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import org.slowcoders.hql.core.antlr.PredicateParser;
import org.slowcoders.hql.core.antlr.PredicateLexer;
import org.slowcoders.hql.core.antlr.PredicateBaseVisitor;

import java.util.ArrayList;
import java.util.List;

class PredicateTranslator extends PredicateBaseVisitor<String> {
    StringBuilder sb = new StringBuilder();
    SqlBuilder relation;
    String paramName;

    PredicateTranslator(SqlBuilder relation, String paramName) {
        this.relation = relation;
        this.paramName = paramName;
    }

    public static String translate(SqlBuilder sqlBuilder, String paramName, String predicate) {
        PredicateLexer lexer = new PredicateLexer(CharStreams.fromString(predicate));
        PredicateParser parser = new PredicateParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.parse();

        PredicateTranslator rewriter = new PredicateTranslator(sqlBuilder, paramName);
        rewriter.sb.append('(');
        tree.getChild(0).accept(rewriter);
        rewriter.sb.append(')');
        return rewriter.sb.toString();
    }


    @Override
    public String visitMacroInvocation(PredicateParser.MacroInvocationContext ctx) {
        String property = ctx.property().getText();
        List<String> callArgs = new ArrayList<>();

        StringBuilder old_sb = this.sb;
        this.sb = new StringBuilder();
        for (PredicateParser.ExprContext arg : ctx.tuple().expr()) {
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
    public String visitProperty(PredicateParser.PropertyContext ctx) {
        String property = ctx.getText();
        String v = relation.resolveProperty(property);
        sb.append(v);
        return "";
    }

    @Override
    public String visitParameter(PredicateParser.ParameterContext ctx) {
        String param = ctx.getText();
        sb.append("#{").append(paramName);
        if (param.length() > 1) {
            sb.append('.');
            sb.append(param.substring(2));
        }
        sb.append('}');
        return param;
    }

    public String visitJoinTargetAttr(PredicateParser.JoinTargetAttrContext ctx) {
        String property = ctx.getText();
        String v = relation.resolveProperty(this.paramName + property.substring(1));
        sb.append(v);
        return "";
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
