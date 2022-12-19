package org.eipgrid.jql.jdbc;

import org.eipgrid.jql.JqlSchema;
import org.eipgrid.jql.JqlSelect;
import org.eipgrid.jql.parser.JqlQuery;

import java.util.Map;

public interface QueryBuilder {
    String createSelectQuery(JqlQuery where, JqlSelect columns);

    String createCountQuery(JqlQuery where);

    String createUpdateQuery(JqlQuery where, Map<String, Object> updateSet);

    String createDeleteQuery(JqlQuery where);

    String prepareFindByIdStatement(JqlSchema schema);

    String createInsertStatement(JqlSchema schema, Map entity, boolean ignoreConflict);
}