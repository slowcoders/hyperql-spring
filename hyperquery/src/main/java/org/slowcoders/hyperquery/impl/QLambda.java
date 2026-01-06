package org.slowcoders.hyperquery.impl;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slowcoders.hql.core.antlr.LambdaBaseVisitor;
import org.slowcoders.hql.core.antlr.LambdaLexer;
import org.slowcoders.hql.core.antlr.LambdaParser;

import java.util.ArrayList;
import java.util.List;

public class QLambda {
    private HSchema relation;
    private String name;
    private final int argCount;
    private final String rawStatement;
    private List<SourceFragment> sourceFragments;

    public QLambda(int argCount, String rawStatement) {
        this.argCount = argCount;
        this.rawStatement = rawStatement;
    }

    public final String getName() { return name; }

    public final int getArgCount() {
        return argCount;
    }

    public final String getRawStatement() {
        return rawStatement;
    }

    public final String inflateStatement(SqlBuilder generator, List<String> args) {
        if (sourceFragments == null) {
            sourceFragments = parseRawStatement(generator);
        }
        StringBuilder sb = new StringBuilder();
        for (SourceFragment sf : sourceFragments) {
            sb.append(sf.getSql(args));
        }
        return sb.toString();
    }

    public void init(HSchema relation, String name) {
        this.relation = relation;
        this.name = name;
    }

    private interface SourceFragment {
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

    private List<SourceFragment> parseRawStatement(SqlBuilder generator) {
        LambdaLexer lexer = new LambdaLexer(CharStreams.fromString(rawStatement));
        LambdaParser parser = new LambdaParser(new CommonTokenStream(lexer));
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

    static class LambdaSplitter extends LambdaBaseVisitor<String> {
        private final SqlBuilder relation;
        SourceBuffer sb = new SourceBuffer();

        public LambdaSplitter(SqlBuilder generator) {
            this.relation = generator;
        }

        @Override
        public String visitMacroInvocation(LambdaParser.MacroInvocationContext ctx) {
            String property = ctx.property().getText();
            List<String> callArgs = new ArrayList<>();

            SourceBuffer old_sb = this.sb;
            for (LambdaParser.ExprContext arg : ctx.tuple().expr()) {
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
        public String visitParameter(LambdaParser.ParameterContext ctx) {
            String idx$ = ctx.getText().substring(1);
            sb.addParam(Integer.parseInt(idx$));
            return "";
        }

        @Override
        public String visitProperty(LambdaParser.PropertyContext ctx) {
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
