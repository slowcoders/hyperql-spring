package org.slowcoders.hyperquery.core;

import org.slowcoders.hyperquery.impl.AliasNode;

public class QAttribute extends AliasNode {

    public QAttribute(String hyperExpr) {
        super(hyperExpr);
    }

    public static QAttribute of(String statement) {
        return new QAttribute(statement);
    }

}
