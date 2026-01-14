package org.slowcoders.hyperquery.impl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slowcoders.hyperquery.core.QJoin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AliasNode {
    private String name;
    private final String encodedExpr;

    // 종속된 Alias(Attr, Lambda, Join) 목록
//    private final HashMap<String, AliasNode> references = new HashMap<>();
//    private final HashMap<String, QJoin> innerJoins = new HashMap<>();
//    int refCount;
    boolean inProgress = false;

    public AliasNode(String encodedExpr) {
        this.encodedExpr = encodedExpr;
    }

    final void setName(String name) {
        this.name = name;
    }

    public final String getName() {
        if (name == null) throw new AssertionError();
        return name;
    }

    public final String getEncodedExpr() {
        return encodedExpr;
    }
    protected synchronized final String inflateStatement(SqlBuilder generator, String paramName) {
        if (inProgress) {
            throw new IllegalStateException("Circular attribute reference is found.");
        }
        try {
            inProgress = true;
            String expr = PredicateTranslator.translate(generator, paramName, encodedExpr);
            return expr;
        } finally {
            inProgress = false;
        }
    }


//    final void addReference(AliasNode alias) {
//        if (!this.references.containsKey(alias.getName())) {
//            alias.refCount++;
//            this.references.put(alias.getName(), alias);
//        }
//    }
//
//    final void addInnerJoin(QJoin join) {
//// this.innerJoins.put(join.getName(), join);
//    }
//
//
//    final HashMap<String, AliasNode> getReferences() {
//        return references;
//    }
//
//    final HashMap<String, QJoin> getInnerJoins() {
//        return innerJoins;
//    }
//
//    protected QJoin getJoin(String alias) {
//        return innerJoins.get(alias);
//    }
//
//    @Override
//    public int hashCode() {
//        return name.hashCode();
//    }

    protected interface SourceFragment {
        String getSql(List<String> args);
        class Text implements SourceFragment {
            String sql;
            public Text(String sql) { this.sql = sql; }
            @Override
            public String getSql(List<String> args) { return sql; }
        }
        class Param implements SourceFragment {
            int index;
            public Param(int index) { this.index = index; }
            @Override
            public String getSql(List<String> args) { return args.get(index - 1); }

            public String toString() { return "\u0000" + index + "\u0001"; }
        }
    }

    protected List<SourceFragment> parseRawStatement(SqlBuilder generator) {
        org.slowcoders.hql.core.antlr.LambdaLexer lexer = new org.slowcoders.hql.core.antlr.LambdaLexer(CharStreams.fromString(getEncodedExpr()));
        org.slowcoders.hql.core.antlr.LambdaParser parser = new org.slowcoders.hql.core.antlr.LambdaParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.expr();

        LambdaSplitter rewriter = new LambdaSplitter(generator);
        tree.accept(rewriter);
        return rewriter.sb.flushText();
    }

    static class SourceBuffer {
        StringBuilder sb = new StringBuilder();
        List<SourceFragment> sourceFragments = new ArrayList<>();
        private static final char ParamNumberStart = '\u0000';
        private static final char ParamNumberEnd = '\u0001';

        void addText(String s) {
            int right = 0;
            for (int p = 0; (p = s.indexOf( ParamNumberStart, right)) >= 0; right ++) {
                sb.append(s.substring(right, p));
                right = s.indexOf( ParamNumberEnd, p);
                int idx = Integer.parseInt(s.substring(p+1, right));
                sourceFragments.add(new SourceFragment.Text(sb.toString()));
                sourceFragments.add(new SourceFragment.Param(idx));
                sb.setLength(0);
            }
            sb.append(s.substring(right));
        }

        void addParam(int index) {
            flushText();
            sourceFragments.add(new SourceFragment.Param(index));
        }

        List<SourceFragment> flushText() {
            if (sb.length() > 0) {
                sourceFragments.add(new SourceFragment.Text(sb.toString()));
                sb.setLength(0);
            }
            return sourceFragments;
        }

        public String toString() {
            flushText();
            StringBuilder sb = new StringBuilder();
            for (SourceFragment sf : sourceFragments) {
                sb.append(sf.toString());
            }
            return sb.toString();
        }
    }

    static class LambdaSplitter extends org.slowcoders.hql.core.antlr.LambdaBaseVisitor<String> {
        private final SqlBuilder relation;
        SourceBuffer sb = new SourceBuffer();

        public LambdaSplitter(SqlBuilder generator) {
            this.relation = generator;
        }

        @Override
        public String visitMacroInvocation(org.slowcoders.hql.core.antlr.LambdaParser.MacroInvocationContext ctx) {
            String property = ctx.property().getText();
            List<String> callArgs = new ArrayList<>();

            SourceBuffer old_sb = this.sb;
            for (org.slowcoders.hql.core.antlr.LambdaParser.ExprContext arg : ctx.tuple().expr()) {
                this.sb = new SourceBuffer();
                arg.accept(this);
                callArgs.add(sb.toString());
            }
            String lambdaCall = relation.resolveLambda(property, callArgs);
            this.sb = old_sb;
            sb.addText(lambdaCall);
            return "";
        }

        @Override
        public String visitParameter(org.slowcoders.hql.core.antlr.LambdaParser.ParameterContext ctx) {
            String idx$ = ctx.getText().substring(1);
            sb.addParam(Integer.parseInt(idx$));
            return "";
        }

        @Override
        public String visitProperty(org.slowcoders.hql.core.antlr.LambdaParser.PropertyContext ctx) {
            String property = ctx.getText();
            String v = relation.resolveProperty(property);
            sb.addText(v);
            return "";
        }

        @Override
        public String visitTerminal(TerminalNode node) {
            sb.addText(node.getText());
            return node.getText();
        }

        @Override
        public String visitErrorNode(ErrorNode node) {
            throw new RuntimeException("QLambda Syntax error: " + node.getText());
        }
    }
}
