package org.slowcoders.hyperquery.impl;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.xml.XMLIncludeTransformer;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.scripting.xmltags.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.SimpleTypeRegistry;
import org.mybatis.spring.SqlSessionTemplate;
import org.slowcoders.hyperquery.core.*;

import java.lang.reflect.Field;
import java.util.*;

public class QStore<E extends QEntity<E>> {
    private final Configuration configuration;
    private final SqlSessionTemplate sqlSessionTemplate;
    private final List<ColumnMapping> columnMappings = new ArrayList<>();
    private final Set<String> joinAliases = new HashSet<>();
    private final Set<Class<QRecord<E>>> usedTables = new HashSet<>();

    static final Map<String, HSchema> joinMap = new HashMap<>();
    private final Class<? extends QRepository> repositoryType;

    public QStore(Configuration configuration, SqlSessionTemplate sqlSessionTemplate, Class<? extends QRepository> repositoryType) {
        this.configuration = configuration;
        this.sqlSessionTemplate = sqlSessionTemplate;
        this.repositoryType = repositoryType;
    }

    public <R extends QRecord<E>> R selectOne(Class<R> resultType, QFilter<E> filter) {
        List<R> res = selectList(resultType, filter);
        if (res.isEmpty()) return null;
        if (res.size() > 1) throw new IllegalStateException("Too many rows returned.");
        return res.get(0);
    }



    public <R extends QRecord<E>> List<R> selectList(Class<R> resultType, QFilter<E> filter) {
        SqlBuilder gen = new SqlBuilder(resultType, filter);
        String sql = gen.build();

        String id = registerMapper(null, gen.getRootSchema(), resultType);

        HashMap<String, Object> params = new HashMap<>();
        params.put("_parameter", filter);
        params.put("__sql__", sql);

        Object res = sqlSessionTemplate.selectList(id, params);
        return (List) res;
    }

    private ResultMap createNestedResultMap(Class<?> clazz, String resultMapId, String propertyPrefix) {
        List<ResultMapping> resultMappings = new ArrayList<>();

        for (Field f : clazz.getDeclaredFields()) {
            String columnName = HModel.Helper.getColumnName(f); // @TColumn 등에서 컬럼명 추출
            if (columnName == null) continue;

            if (HModel.Helper.isCollectionType(f)) {
                // 1:N Collection 매핑 처리
                Class<?> listItemType = HModel.Helper.getElementType(f); // List의 제네릭 타입 추출 (예: HpcaTransactionPost)

                // 자식 엔티티를 위한 중첩 ResultMap 생성/참조
                String nestedMapId = resultMapId + "." + f.getName();
                ResultMap nestedMap = createNestedResultMap(listItemType, nestedMapId, propertyPrefix + f.getName() + '.');
                if (!configuration.hasResultMap(nestedMapId)) {
                    configuration.addResultMap(nestedMap);
                }

                ResultMapping mapping = new ResultMapping.Builder(configuration, f.getName())
                        .javaType(f.getType())
                        .notNullColumns(nestedMap.getMappedColumns())
                        .columnPrefix(f.getName() + '.')
                        .nestedResultMapId(nestedMapId) // 핵심: 자식 매핑 ID 연결
                        .build();
                resultMappings.add(mapping);
            } else if (true || propertyPrefix.isEmpty() /* top level only ?? */) {
                // 일반 컬럼 매핑 (ID 또는 Result)
                boolean isPK = HModel.Helper.isUniqueKey(f);
                ResultMapping mapping = new ResultMapping.Builder(configuration, f.getName(), f.getName(), f.getType())
                        .flags(isPK ? Collections.singletonList(ResultFlag.ID) : Collections.emptyList())
                        .build();
                resultMappings.add(mapping);
            }
        }

        return new ResultMap.Builder(configuration, resultMapId, clazz, resultMappings, true).build();
    }
    
    String registerMapper(SqlSource sqlSource, HSchema relation, Class<?> resultType) {
        String id = repositoryType.getName() + ".__select__." + resultType.getName();

        if (!configuration.hasStatement(id)) {
            ResultMap inlineResultMap = createNestedResultMap(resultType, id + "-Inline", ""); 
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

    static class ScriptBuilder extends XMLScriptBuilder {
        private final XNode rootSqlNode;

        ScriptBuilder(Configuration configuration, XNode rootSqlNode) {
            super(configuration, rootSqlNode);
            this.rootSqlNode = rootSqlNode;
        }

        public SqlSource parseScriptNode(Object parameterObject) {
            if (false) {
                super.parseScriptNode();
            }
            MixedSqlNode nodes = super.parseDynamicTags(rootSqlNode);
            return buildScriptNode(nodes, parameterObject);
        }

        public SqlSource buildScriptNode(SqlNode nodes, Object parameterObject) {
            DynamicContext context = new DynamicContext(configuration, parameterObject);
            nodes.apply(context);
            String sql = context.getSql();
            // context 의 sql 을 변경하고, 다시 작업.
            GenericTokenParser parser = new GenericTokenParser("#{", "}", new TokenHandler() {

                @Override
                public String handleToken(String content) {
                    Object parameter = context.getBindings().get("_parameter");
                    if (parameter == null) {
                        context.getBindings().put("value", null);
                    } else if (SimpleTypeRegistry.isSimpleType(parameter.getClass())) {
                        context.getBindings().put("value", parameter);
                    }
                    Object value = OgnlCache.getValue(content, context.getBindings());
                    return String.valueOf(value); // issue #274 return "" instead of "null"
                }
            });
            String converted = parser.parse(sql);
            TextSqlNode textNode = new TextSqlNode(converted);
            return new RawSqlSource(configuration, textNode, parameterObject.getClass());
        }
    }

}