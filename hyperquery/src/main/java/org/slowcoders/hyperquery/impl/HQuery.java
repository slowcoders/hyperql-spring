package org.slowcoders.hyperquery.impl;

import org.apache.ibatis.parsing.XNode;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class HQuery {
    XNode with;
    String query;

    public HQuery(XNode rootSqlNode, String sql) {
        this.query = sql;
        this.with = rootSqlNode;
    }

    public String toString() {
        if (with == null) {
            return query;
        }

        try {
            StringWriter sw = new StringWriter();
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(with.getNode()), new StreamResult(sw));
            String sql = sw.toString();
            sql = sql.replaceAll("&quot;", "\"");
            return sql.replace("${__sql__}", query);
        } catch (Exception e) {
            return "Error printing node: " + e.getMessage();
        }
    }
}
