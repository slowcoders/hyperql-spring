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
    private HSchema schema;

    // 종속된 Alias(Attr, Lambda, Join) 목록
//    private final HashMap<String, AliasNode> references = new HashMap<>();
//    private final HashMap<String, QJoin> innerJoins = new HashMap<>();
//    int refCount;
    boolean inProgress = false;

    public AliasNode(String encodedExpr) {
        this.encodedExpr = encodedExpr;
    }

    final void setName(HSchema schema, String name) {
        this.schema = schema;
        this.name = name;
    }

    public final String getName() {
        if (name == null) throw new AssertionError();
        return name;
    }

    public final HSchema getSchema() {
        return schema;
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

}
