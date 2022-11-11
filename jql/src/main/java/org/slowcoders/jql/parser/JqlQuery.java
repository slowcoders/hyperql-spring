package org.slowcoders.jql.parser;

import org.slowcoders.jql.JqlEntityJoin;
import org.slowcoders.jql.JqlSchema;

import java.util.ArrayList;
import java.util.List;

public class JqlQuery extends TableQuery {

    private final ArrayList<JqlEntityJoin> fkJoins = new ArrayList<>();
    private final ArrayList<JqlEntityJoin> pkJoins = new ArrayList<>();
    private final ArrayList<JqlResultMapping> resultMappings = new ArrayList<>();
    private static final String[] emptyJsonPath = new String[0];

    public JqlQuery(JqlSchema schema) {
        super(null, schema);
        this.resultMappings.add(new JqlResultMapping(emptyJsonPath, schema));
    }

    public JqlQuery getTopQuery() {
        return this;
    }

    protected JqlSchema addTableJoin(JqlEntityJoin join, boolean fetchData) {
        String jsonKey = join.getJsonKey();
        if (!join.isInverseMapped()) {
            if (fkJoins.indexOf(join) < 0) {
                fkJoins.add(join);
            }
        } else {
            if (pkJoins.indexOf(join) < 0) {
                pkJoins.add(join);
            }
            if (join.getAssociateJoin() != null) {
                join = join.getAssociateJoin();
//                if (pkJoins.indexOf(join) < 0) {
//                    pkJoins.add(join);
//                }
            }
        }
        JqlSchema schema = join.getJoinedSchema();
        if (fetchData && !isAlreadyFetched(schema)) {
            String[] basePath = getJsonPath(join.getBaseSchema());
            String[] jsonPath = toJsonPath(basePath, jsonKey);
            this.resultMappings.add(new JqlResultMapping(jsonPath, schema));
        }
        return schema;
    }

    private boolean isAlreadyFetched(JqlSchema schema) {
        for (JqlResultMapping fi : resultMappings) {
            if (fi.getSchema() == schema) return true;
        }
        return false;
    }

    private String[] getJsonPath(JqlSchema anchorSchema) {
        for (JqlResultMapping fetch : resultMappings) {
            if (fetch.getSchema() == anchorSchema) {
                return fetch.getJsonPath();
            }
        }
        return null;
    }

    private String[] toJsonPath(String[] basePath, String jsonKey) {
        String[] path = jsonKey.split("\\.");
        if (basePath != null && basePath.length > 0) {
            String[] jsonPath = new String[basePath.length + path.length];
            System.arraycopy(basePath, 0, jsonPath, 0, basePath.length);
            System.arraycopy(path, 0, jsonPath, basePath.length, path.length);
            path = jsonPath;
        }
        return path;
    }


    public List<JqlEntityJoin> getForeignKeyBasedJoins() {
        return fkJoins;
    }

    public List<JqlEntityJoin> getPrimaryKeyBasedJoins() {
        return pkJoins;
    }

    public List<JqlResultMapping> getResultMappings() {
        return this.resultMappings;
    }

}