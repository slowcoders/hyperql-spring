package org.slowcoders.hyperquery.core;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slowcoders.hql.core.antlr.PredicateBaseVisitor;
import org.slowcoders.hql.core.antlr.PredicateLexer;
import org.slowcoders.hql.core.antlr.PredicateParser;
import org.slowcoders.hyperquery.impl.HqRelation;
import org.slowcoders.hyperquery.impl.QLambda;

import java.util.ArrayList;
import java.util.List;

public class QueryParser {


    public String parsePredicate(HqRelation relation, String paramName, String predicate) {
        PredicateLexer lexer = new PredicateLexer(CharStreams.fromString(predicate));
        PredicateParser parser = new PredicateParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.expr();

        PredicateRewriter rewriter = new PredicateRewriter(relation, paramName);
        tree.accept(rewriter);
        return rewriter.sb.toString();
    }

    static class PredicateRewriter extends PredicateBaseVisitor<String> {
        StringBuilder sb = new StringBuilder();
        HqRelation relation;
        String paramName;

        PredicateRewriter(HqRelation relation, String paramName) {
            this.relation = relation;
            this.paramName = paramName;
        }


        @Override
        public String visitMacroInvocation(PredicateParser.MacroInvocationContext ctx) {
            String property = ctx.property().getText();
            String name = property.substring(property.indexOf('.')+1);
            String[] joinPath = property.substring(0, property.indexOf('.')).split("@");

            QLambda lambda = relation.getLambda(joinPath, name);
            List<PredicateParser.ExprContext> args = ctx.tuple().expr();
            StringBuilder old_sb = this.sb;
            this.sb = new StringBuilder();
            List<String> callArgs = new ArrayList<>();
            for (PredicateParser.ExprContext arg : args) {
                sb.setLength(0);
                arg.accept(this);
                callArgs.add(sb.toString());
            }
            String lambdaCall = lambda.inflateStatement(callArgs);
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
}
