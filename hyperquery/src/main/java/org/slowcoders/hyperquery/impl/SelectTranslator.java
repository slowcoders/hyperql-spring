package org.slowcoders.hyperquery.impl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

class SelectTranslator extends org.slowcoders.hql.core.antlr.SelectionBaseVisitor<String> {
    static Pattern Identifier = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
    StringBuilder sb = new StringBuilder();
    SqlBuilder relation;
    String paramName;

    SelectTranslator(SqlBuilder relation, String paramName) {
        this.relation = relation;
        this.paramName = paramName;
    }

    public static String translate(SqlBuilder sqlBuilder, String paramName, String expression) {
        if (Identifier.matcher(expression).matches()) {
            return sqlBuilder.resolveProperty("@." + expression);
        }
        org.slowcoders.hql.core.antlr.SelectionLexer lexer = new org.slowcoders.hql.core.antlr.SelectionLexer(CharStreams.fromString(expression));
        org.slowcoders.hql.core.antlr.SelectionParser parser = new org.slowcoders.hql.core.antlr.SelectionParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.expr();

        SelectTranslator rewriter = new SelectTranslator(sqlBuilder, paramName);
        tree.accept(rewriter);
        return rewriter.sb.toString();
    }

    @Override
    public String visitMacroInvocation(org.slowcoders.hql.core.antlr.SelectionParser.MacroInvocationContext ctx) {
        String property = ctx.property().getText();
        List<String> callArgs = new ArrayList<>();

        StringBuilder old_sb = this.sb;
        this.sb = new StringBuilder();
        for (org.slowcoders.hql.core.antlr.SelectionParser.ExprContext arg : ctx.tuple().expr()) {
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
    public String visitProperty(org.slowcoders.hql.core.antlr.SelectionParser.PropertyContext ctx) {
        String property = ctx.getText();
        String v = relation.resolveProperty(property);
        sb.append(v);
        return "";
    }

    @Override
    public String visitMapperParameter(org.slowcoders.hql.core.antlr.SelectionParser.MapperParameterContext ctx) {
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
