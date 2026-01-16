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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.Field;
import java.util.*;

public class QStore<T> implements ViewResolver {
    private final Configuration configuration;
    private final SqlSessionTemplate sqlSessionTemplate;
    private final Class<? extends QRepository> repositoryType;

    public QStore(Configuration configuration, SqlSessionTemplate sqlSessionTemplate, Class<? extends QRepository> repositoryType) {
        this.configuration = configuration;
        this.sqlSessionTemplate = sqlSessionTemplate;
        this.repositoryType = repositoryType;
    }

    public <E extends QEntity<E>, R extends QRecord<E>> R selectOne(Class<R> resultType, QFilter<E> filter) {
        List<R> res = selectList(resultType, filter);
        if (res.isEmpty()) return null;
        if (res.size() > 1) throw new IllegalStateException("Too many rows returned.");
        return res.get(0);
    }


    public <E extends QEntity<E>, R extends QRecord<E>> List<R> selectList(HModel view, Class<R> resultType, QFilter<E> filter) {
        SqlBuilder gen = new SqlBuilder(view, resultType, filter, this);
        String sql = gen.build();

        String id = registerMapper(null, gen.getRootSchema(), resultType);

        HFilter._sql.set(sql);
        HFilter._session.set(getCurrentSessionInfo());

        Object res = sqlSessionTemplate.selectList(id, filter);
        return (List) res;
    }

    public <E extends QEntity<E>, R extends QRecord<E>> List<R> selectList(Class<R> resultType, QFilter<E> filter) {
        return selectList(HSchema.getSchema(resultType, false), resultType, filter);
    }

    public Object getCurrentSessionInfo() {
        return null;
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
    
    String registerMapper(SqlSource sqlSource, HModel relation, Class<?> resultType) {
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

    public String resolveView(String namespace, String sqlFragmentId, Map<String, String> properties) {
        String mapperId = namespace + "." + sqlFragmentId;
        if (configuration.hasStatement(mapperId)) {
            throw new IllegalArgumentException("Only <sql> fragments can be used to create View.");
        }

        // <include> 처리.
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration, "???");
        builderAssistant.setCurrentNamespace(namespace);
        XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, builderAssistant);
        XNode fr = configuration.getSqlFragments().get(mapperId);
        includeParser.applyIncludes(fr.getNode());
        if (!checkValidXmlQuery(fr.getNode())) {
            throw new IllegalArgumentException("View expression should not contain conditional expression like <if>, <choose>, <foreach>. But current is \n" + fr.toString());
        }

        String sql = fr.getNode().getTextContent();
        sql = new GenericTokenParser("${", "}", properties::get).parse(sql);
        return sql;
    }

    private boolean checkValidXmlQuery(Node node) {
        switch (node.getNodeName()) {
            case "if": case "choose": case "when": case "foreach":
                return false;
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            checkValidXmlQuery(children.item(i));
        }
        return true;
    }
}