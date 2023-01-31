package org.eipgrid.jql.jdbc;

import io.swagger.v3.oas.annotations.Operation;
import org.eipgrid.jql.jdbc.metadata.JdbcSchema;
import org.eipgrid.jql.js.JsUtil;
import org.eipgrid.jql.schema.QColumn;
import org.eipgrid.jql.schema.QJoin;
import org.eipgrid.jql.schema.QSchema;
import org.eipgrid.jql.util.KVEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Map;

public abstract class JdbcMetadataController {

    private final JdbcJqlService service;

    public JdbcMetadataController(JdbcJqlService service) {
        this.service = service;
    }


    private QSchema getSchema(String db_schema, String tableName) throws Exception {
        String tablePath = service.makeTablePath(db_schema, tableName);
//        JqlRepository repo = service.getRepository(tablePath);
//        if (repo.getEntityType() != JqlEntity.class) {
//            return service.loadSchema(repo.getEntityType());
//        }
        return service.loadSchema(tablePath, null);
    }

    @GetMapping("/{schema}/{table}")
    @ResponseBody
    @Operation(summary = "table column 목록")
    public Map columns(@PathVariable("schema") String db_schema,
                                @PathVariable("table") String tableName) throws Exception {
        QSchema schema = getSchema(db_schema, tableName);
        ArrayList<String> columns = new ArrayList<>();
        for (QColumn column : schema.getPrimitiveColumns()) {
            columns.add(column.getJsonKey());
        }
        ArrayList<String> refs = new ArrayList<>();
        for (QColumn column : schema.getObjectColumns()) {
            refs.add(column.getJsonKey());
        }
        for (Map.Entry<String, QJoin> entry : schema.getEntityJoinMap().entrySet()) {
            if (!entry.getValue().getTargetSchema().hasOnlyForeignKeys()) {
                refs.add(entry.getKey());
            }
        }
        KVEntity entity = KVEntity.of("columns", columns);
        if (refs.size() > 0) {
            entity.put("references", refs);
        }
        return entity;
    }




    @GetMapping("/{schema}/{table}/{type}")
    @ResponseBody
    @Operation(summary = "Schema 소스 생성")
    public String jsonSchema(@PathVariable("schema") String db_schema,
                             @PathVariable("table") String tableName,
                             @PathVariable("type") SchemaType type) throws Exception {
        if ("*".equals(tableName)) {
            return jpaSchemas(db_schema, type);
        }

        QSchema schema = getSchema(db_schema, tableName);
        String source;
        if (type == SchemaType.Simple) {
            source = JsUtil.getSimpleSchema(schema);
        }
        else if (type == SchemaType.Javascript) {
            source = JsUtil.createDDL(schema);
            String join = JsUtil.createJoinJQL(schema);
            StringBuilder sb = new StringBuilder();
            sb.append(source).append("\n\n").append(join);
            source = sb.toString();
        }
        else {
            if (schema instanceof JdbcSchema) {
                source = ((JdbcSchema)schema).dumpJPAEntitySchema();
            }
            else {
                source = tableName + " is not a JdbcSchema";
            }
        }
        return source;
    }

    private String jpaSchemas(@PathVariable("schema") String db_schema,
                             @PathVariable("type") SchemaType type) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String tableName : service.getTableNames(db_schema)) {
            QSchema schema = getSchema(db_schema, tableName);
            if (type == SchemaType.Javascript) {
                String source = JsUtil.createDDL(schema);
                String join = JsUtil.createJoinJQL(schema);
                sb.append(source).append("\n\n").append(join);
            } else if (schema instanceof JdbcSchema) {
                String source = ((JdbcSchema) schema).dumpJPAEntitySchema();
                sb.append(source);
            } else {
                continue;
            }
            sb.append("\n\n//-------------------------------------------------//\n\n");
        }
        return sb.toString();
    }

    @GetMapping("/{schema}")
    @ResponseBody
    @Operation(summary = "Table 목록")
    public String listTables(@PathVariable("schema") String db_schema) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String tableName : service.getTableNames(db_schema)) {
            sb.append(tableName).append('\n');
        }
        return sb.toString();
    }

    @GetMapping("/")
    @ResponseBody
    @Operation(summary = "DB schema 목록")
    public String listSchemas() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String tableName : service.getDBSchemas()) {
            sb.append(tableName).append('\n');
        }
        return sb.toString();
    }

    enum SchemaType {
        Simple,
        Javascript,
        SpringJPA
    }
}
