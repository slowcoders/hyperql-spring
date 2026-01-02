package org.slowcoders.hyperquery.impl;

import jakarta.persistence.Column;
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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public class QStore<E extends QRecord<E>> {
    private final Configuration configuration;
    private final SqlSessionTemplate sqlSessionTemplate;
    private final List<ColumnMapping> columnMappings = new ArrayList<>();
    private final Set<String> joinAliases = new HashSet<>();
    private final Set<Class<QRecord<E>>> usedTables = new HashSet<>();

    static final Map<String, HSchema> joinMap = new HashMap<>();
    private final Class<? extends QRepository> repositoryType;

    private static class ColumnMapping {
        final String columnName;
        final String fieldName;
        public ColumnMapping(String columnName, String fieldName) {
            this.columnName = columnName;
            this.fieldName = fieldName;
        }
    }
    public QStore(Configuration configuration, SqlSessionTemplate sqlSessionTemplate, Class<? extends QRepository> repositoryType) {
        this.configuration = configuration;
        this.sqlSessionTemplate = sqlSessionTemplate;
        this.repositoryType = repositoryType;
    }

    static class Helper implements QEntity<Helper> {
        static String getColumnName(Field f) {
            QColumn anno = f.getAnnotation(QColumn.class);
            if (anno != null) return anno.value();

            PKColumn pk = f.getAnnotation(PKColumn.class);
            if (pk != null) return pk.value();

            QEntity.TColumn tcol = f.getAnnotation(QEntity.TColumn.class);
            if (tcol != null) return tcol.value();

            Column col = f.getAnnotation(Column.class);
            if (col != null) return col.name();

            return null;
        }

        public static boolean isUniqueKey(Field f) {
            return f.getAnnotation(QEntity.PKColumn.class) != null;
        }
    }

    private static Class<?> getElementType(Field f) {
        if (f.getType().isArray()) return f.getType().getComponentType();

        Type type = f.getGenericType();
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            Type[] typeArguments = pType.getActualTypeArguments();
            if (typeArguments.length > 0 && typeArguments[0] instanceof Class) {
                return (Class<?>) typeArguments[0];
            }
        }
        return f.getType();
    }

    private static boolean isCollectionType(Field f) {
        return f.getType().isArray() || Collection.class.isAssignableFrom(f.getType());
    }
    private void addSelection(Class<?> clazz, String propertyPrefix, String fromAlias) {
        for (Field f : clazz.getDeclaredFields()) {
            String columnName = Helper.getColumnName(f);
            if (columnName == null) continue;
            Class<?> elementType = getElementType(f);
            if (!QRecord.class.isAssignableFrom(elementType)) {
                int idx_dot = columnName.indexOf(".");
                if (idx_dot > 0) {
                    String alias = columnName.substring(0, idx_dot);
                    joinAliases.add(alias);
                } else {
                    columnName = fromAlias + '.' + columnName;
                }
                if (isCollectionType(f)) {

                }
                columnMappings.add(new ColumnMapping(columnName, propertyPrefix + f.getName()));
            } else if (!usedTables.contains(elementType)) {
                // join loop 방지.
                usedTables.add((Class<QRecord<E>>) elementType);
                if (isCollectionType(f)) {

                }
                String alias = propertyPrefix.isEmpty() ? columnName : fromAlias + columnName;
                this.addSelection(elementType, propertyPrefix + f.getName() + '.', alias);
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

        HashMap<String, HModel> ctes = new HashMap<>();
        HSchema relation = filter.getRelation();
        sb.append("from ").append(relation.getTableName()).append(" @").append('\n');
        for (String alias : joinAliases) {
            QJoin join = relation.getJoin(alias);
            String tableName = join.getTargetRelation().getTableName();
            if (tableName.length() == 0) {
                ctes.put(alias, join.getTargetRelation());
            }
            sb.append("left join ").append(tableName).append(" ").append(alias);
            // replace #*. -> "@" + join + "."
            String joinCriteria = join.getJoinCriteria().replace("#", alias);
            int lastDot = alias.lastIndexOf('@');
            if (lastDot > 0) {
                joinCriteria = joinCriteria.replaceAll("@\\.", alias.substring(0, lastDot) + ".");
            }
            sb.append("\n on ").append(joinCriteria).append('\n');
        }

        String sql = sb.append("where ").append(filter.toString()).toString();
        if (ctes.size() > 0) {
            StringBuilder sb2 = new StringBuilder("WITH ");
            for (Map.Entry<String, HModel> entry : ctes.entrySet()) {
                sb2.append(entry.getKey()).append(" AS (\n");
                sb2.append(entry.getValue().getQuery()).append("), ");
            }
            sb2.setLength(sb2.length() - 2);
            sql = sb2.append('\n').append(sql).toString();
        }

        String[] joins__ = joinAliases.toArray(new String[joinAliases.size()]);
        for (int idxAlias = joins__.length; --idxAlias >= 0; ) {
            String join = joins__[idxAlias];
            String alias = join.substring(join.lastIndexOf('@') + 1);
            sql = sql.replaceAll(join + "\\b", alias + "_" + (idxAlias));
        }
        sql = sql.replaceAll("@(?=\\W)", "t_0");


        filter.setSql(sql);
        String id = registerMapper(null, filter.getRelation(), resultType);

        Object res = sqlSessionTemplate.selectList(id, filter);
        return (List) res;
    }

    private ResultMap createNestedResultMap(Class<?> clazz, String resultMapId, String propertyPrefix) {
        List<ResultMapping> resultMappings = new ArrayList<>();

        for (Field f : clazz.getDeclaredFields()) {
            String columnName = Helper.getColumnName(f); // @TColumn 등에서 컬럼명 추출
            if (columnName == null) continue;

            if (isCollectionType(f)) {
                // 1:N Collection 매핑 처리
                Class<?> listItemType = getElementType(f); // List의 제네릭 타입 추출 (예: HpcaTransactionPost)

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
                boolean isPK = Helper.isUniqueKey(f);
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