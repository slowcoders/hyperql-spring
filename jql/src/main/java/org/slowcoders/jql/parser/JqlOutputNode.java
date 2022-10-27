package org.slowcoders.jql.parser;

import org.slowcoders.jql.JqlColumn;
import org.slowcoders.jql.JqlSchema;

import java.util.List;

public class JqlOutputNode {
    private final String[] jsonPath;
    private final JqlSchema schema;
    private List<JqlColumn> columns;

    public JqlOutputNode(String[] jsonPath, JqlSchema schema) {
        this(jsonPath, schema, null);
    }

    public JqlOutputNode(String[] jsonPath, JqlSchema schema, List<JqlColumn> columns) {
        this.jsonPath = jsonPath;
        this.schema = schema;
        this.columns = columns;
    }

    public String[] getJsonPath() {
        return jsonPath;
    }

    public JqlSchema getSchema() {
        return schema;
    }

    public List<JqlColumn> getColumnList() {
        return columns;
    }
}
