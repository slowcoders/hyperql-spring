package org.slowcoders.hyperquery.impl;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.xml.XMLIncludeTransformer;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.xmltags.XMLScriptBuilder;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionTemplate;
import org.slowcoders.hyperquery.core.*;

import java.lang.reflect.Field;
import java.util.*;

public class QStore<E extends QEntity> {
    private final Configuration configuration;
    private final SqlSessionTemplate sqlSessionTemplate;
    private final List<ColumnMapping> columnMappings = new ArrayList<>();
    private final Set<String> joinAliases = new HashSet<>();
    private final Set<Class<QRecord>> usedTables = new HashSet<>();

    static final Map<String, HSchema> joinMap = new HashMap<>();
    private final Class<? extends QRepository<E>> repositoryType;

    static class ColumnMapping {
        private final String columnName;
        private final String fieldName;
        public ColumnMapping(String columnName, String fieldName) {
            this.columnName = columnName;
            this.fieldName = fieldName;
        }
    }
    public QStore(Configuration configuration, SqlSessionTemplate sqlSessionTemplate, Class<? extends QRepository<E>> repositoryType) {
        this.configuration = configuration;
        this.sqlSessionTemplate = sqlSessionTemplate;
        this.repositoryType = repositoryType;
    }

    private void addSelection(Class<?> clazz, String propertyPrefix, String fromAlias) {
        for (Field f : clazz.getDeclaredFields()) {
            QColumn anno = f.getAnnotation(QColumn.class);
            if (anno == null) continue;
            String columnName = anno.value();
            Class<?> propertyType = f.getType();
            if (!QRecord.class.isAssignableFrom(propertyType)) {
                int idx_dot = columnName.indexOf(".");
                if (idx_dot > 0) {
                    String alias = columnName.substring(0, idx_dot);
                    joinAliases.add(alias);
                } else {
                    columnName = fromAlias + '.' + columnName;
                }
                columnMappings.add(new ColumnMapping(columnName, propertyPrefix + f.getName()));
            } else if (!usedTables.contains(propertyType)) {
                // join loop 방지.
                usedTables.add((Class<QRecord>) propertyType);
                String alias = propertyPrefix.isEmpty() ? columnName : fromAlias + '@' + columnName;
                this.addSelection(propertyType, propertyPrefix + f.getName() + '.', alias);
                joinAliases.add(alias);
            }
        }
    }

    public <R extends QRecord<E>> R selectOne(Class<R> resultType, QFilter<E> filter) {
        List<R> res = selectList(resultType, filter);
        if (res.isEmpty()) return null;
        if (res.size() > 1) throw new IllegalStateException("Too many rows returned.");
        return res.get(0);
    }

    public <R extends QRecord<E>> List<R> selectList(Class<R> resultType, QFilter<E> filter) {
        this.addSelection(resultType, "", "@");

        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        for (ColumnMapping col : columnMappings) {
            sb.append(col.columnName).append(" as \"").append(col.fieldName).append("\",\n");
        }
        sb.setLength(sb.length() - 2);
        sb.append('\n');

        HSchema relation = filter.getRelation();
        sb.append("from ").append(relation.getJoinTarget()).append(" @").append('\n');
        for (String alias : joinAliases) {
            QJoin join = relation.getJoin(alias);
            sb.append("left join ").append(join.getTargetRelation().getQuery()).append(" ").append(alias);
            // replace #*. -> "@" + join + "."
            sb.append("\n on ").append(join.getJoinCriteria().replace("#", alias)).append('\n');
        }

        String sql = sb.append("where ").append(filter.toString()).toString();
        int idxAlias = 0;
        for (String join : joinAliases) {
            String alias = join.substring(join.lastIndexOf('@') + 1);
            sql = sql.replaceAll(join + "\\b", alias + "_" + (++idxAlias));
        }
        sql = sql.replaceAll("@(?=\\W)", "t_0");
        filter.setSql(sql);
        String id = registerMapper(null, filter.getRelation(), resultType);

        Object res = sqlSessionTemplate.selectList(id, filter);
        return (List) res;
    }

    String registerMapper(SqlSource sqlSource, HSchema relation, Class<?> resultType) {
        String id = repositoryType.getName() + ".__select__." + resultType.getName();

        if (!configuration.hasStatement(id)) {
            ResultMap inlineResultMap = new ResultMap.Builder(configuration, id + "-Inline", resultType,
                    new ArrayList<>(), null).build();
            List<ResultMap> __resultMaps = new ArrayList<>();
            __resultMaps.add(inlineResultMap);

            String root_id = repositoryType.getName() + ".__select__";
            MappedStatement root_ms = configuration.getMappedStatement(root_id);
            if (sqlSource == null) sqlSource = root_ms.getSqlSource();
            MappedStatement.Builder builder = new MappedStatement.Builder(configuration, id, sqlSource, root_ms.getSqlCommandType())
                    .resource(root_ms.getResource())
                    .fetchSize(root_ms.getFetchSize())
                    .timeout(root_ms.getTimeout())
                    .statementType(root_ms.getStatementType())
                    .keyGenerator(root_ms.getKeyGenerator())
//                        .keyProperty(root_ms.getKeyProperties())
//                        .keyColumn(root_ms.getKeyColumns())
                    .databaseId(root_ms.getDatabaseId())
                    .lang(root_ms.getLang())
                    .resultOrdered(root_ms.isResultOrdered())
//                        .resultSets(root_ms.getResultSets())
                    .parameterMap(root_ms.getParameterMap())
                    .resultMaps(__resultMaps)
                    .resultSetType(root_ms.getResultSetType())
                    .flushCacheRequired(root_ms.isFlushCacheRequired())
                    .useCache(root_ms.isUseCache())
                    .cache(root_ms.getCache());
//                    .dirtySelect(root_ms.isDirtySelect());

            configuration.addMappedStatement(builder.build());
        }
        return id;
    }

    public String getSqlNode(Class<?> mapperClass, String sqlFragmentId) {
        // <include> 처리.
        String mapperId = mapperClass.getName() + "." + sqlFragmentId;
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration, mapperClass.getName() + ".???");
        XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, builderAssistant);
        builderAssistant.setCurrentNamespace(mapperClass.getName());

        XNode fr = configuration.getSqlFragments().get(mapperId);
        includeParser.applyIncludes(fr.getNode());
        return fr.getStringBody();
    }

    public void getViewMapper(Class<?> mapperClass, Class<? extends QRecord<?>> resultType) throws Exception {
//        Class<?> mapperClass = null // UserMapper.class;
        String mapperName = "blockDetailOnDate";
        HashMap<String, Object> mapperParams = new HashMap<>();
        mapperParams.put("selections", "*");
        mapperParams.put("date", "2025-02-08");
        String mapperId = mapperClass.getName() + "." + mapperName;

        if (configuration.hasStatement(mapperId)) {
            MappedStatement ms = configuration.getMappedStatement(mapperId);
            SqlSource ss = ms.getSqlSource();
            ss.getBoundSql(mapperParams);
            throw new IllegalArgumentException("Only <sql> fragments can be used to create View.");
        } else {

            // <include> 처리.
            MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration, mapperClass.getName() + ".???");
            XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, builderAssistant);
            builderAssistant.setCurrentNamespace(mapperClass.getName());

            XNode fr = configuration.getSqlFragments().get(mapperId);
            includeParser.applyIncludes(fr.getNode());

            // <parameter 처리>
            SqlSource sqlSource;
            if (false) {
//                sqlSource = new ScriptBuilder(configuration, fr).parseScriptNode(mapperParams);
            }
            else {
                sqlSource = new XMLScriptBuilder(configuration, fr).parseScriptNode();
            }

            final boolean checkDynamicParameter = true;
            if (checkDynamicParameter) {
                BoundSql bsql = sqlSource.getBoundSql(mapperParams);
                if (!bsql.getParameterMappings().isEmpty()) {
                    throw new IllegalArgumentException("View statement should not contain dynamic parameters. " + bsql.getParameterMappings());
                }
            }

            mapperId = registerMapper(sqlSource, HSchema.registerSchema(null), resultType);
        }

        Object res = sqlSessionTemplate.selectList(mapperId, mapperParams);

        System.out.println(res);
    }
}