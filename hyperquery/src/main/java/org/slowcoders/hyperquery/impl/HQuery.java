package org.slowcoders.hyperquery.impl;

import org.apache.ibatis.parsing.XNode;

public class HQuery {
    XNode with;
    String query;

    public HQuery(XNode rootSqlNode, String sql) {
        this.query = sql;
        this.with = rootSqlNode;
    }
}
