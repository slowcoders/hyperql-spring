package org.slowcoders.hyperquery.core;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slowcoders.hql.core.antlr.LambdaBaseVisitor;
import org.slowcoders.hql.core.antlr.LambdaLexer;
import org.slowcoders.hql.core.antlr.LambdaParser;
import org.slowcoders.hyperquery.impl.AliasNode;
import org.slowcoders.hyperquery.impl.SqlBuilder;

import java.util.ArrayList;
import java.util.List;

public class QLambda extends AliasNode {
    private final int argCount;
    private List<SourceFragment> sourceFragments;

    public QLambda(int argCount, String rawStatement) {
        super(rawStatement);
        this.argCount = argCount;
    }


    public final int getArgCount() {
        return argCount;
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


}
