package org.slowcoders.hyperquery.impl;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.xml.XMLIncludeTransformer;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.xmltags.*;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionTemplate;
import org.slowcoders.hyperquery.core.*;
import org.slowcoders.hyperquery.util.KVEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.util.ClassUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.*;

public class QStore<T> implements ViewResolver, JdbcConnector {
    private final Configuration configuration;
    private final SqlSessionTemplate sqlSessionTemplate;
    private final Class<? extends QRepository> repositoryType;
    private final HSchema schema;

    public QStore(Configuration configuration, SqlSessionTemplate sqlSessionTemplate, QRepository repository) {
        this.configuration = configuration;
        this.sqlSessionTemplate = sqlSessionTemplate;
        this.schema = HSchema.loadSchema(this.getClass(), true, this);
        Class<?> repositoryType = null;
        for (Class<?> iface : ClassUtils.getUserClass(repository).getInterfaces()) {
            if (QRepository.class.isAssignableFrom(iface)) {
                repositoryType = (Class) iface;
                break;
            }
        }
        this.repositoryType = (Class<? extends QRepository>) repositoryType;
        if (repositoryType == null) {
            throw new IllegalArgumentException("Repository must implement QRepository interface.");
        }
    }

    public <E extends QEntity<E>, R extends QRecord<E>> R selectOne(Class<R> resultType, QFilter<E> filter) {
        List<R> res = selectList(resultType, filter);
        if (res.isEmpty()) return null;
        if (res.size() > 1) throw new IllegalStateException("Too many rows returned.");
        return res.get(0);
    }


    public <E extends QEntity<E>, R extends QRecord<E>> List<R> selectList(HModel view, Class<R> resultType, QFilter<E> filter) {
        HSchema viewSchema = view.loadSchema(this);
        if (filter != null && loadSchema(filter.getClass(), false) != viewSchema) {
            throw new IllegalArgumentException("Filter type is not related to result type.");
        }

        SqlBuilder gen = new SqlBuilder(view, this);
        HQuery query = gen.buildSelect(resultType, filter);

        String id = registerMapper(query.with, resultType);

        HFilter._sql.set(query.query);
        HFilter._session.set(getCurrentSessionInfo());

        try {
            String sql = query.toString();
            Object res = sqlSessionTemplate.selectList(id, filter);
            return (List) res;
        } catch (RuntimeException e) {
            System.out.println("Execution failed\n" + query.toString());
            throw e;
        }
    }

    public <E extends QEntity<E>, R extends QRecord<E>> List<R> selectList(Class<R> resultType, QFilter<E> filter) {
        return selectList(loadSchema(resultType, false), resultType, filter);
    }

    public <E extends QEntity<E>> int insert(QEntity<E> entity, boolean updateOnConflict) {
        HSchema schema = loadSchema(entity.getClass(), false);
        SqlBuilder gen = new SqlBuilder(schema, this);
        String query = gen.buildInsert(entity, updateOnConflict);

        String id = repositoryType.getName() + ".__insert__";

        QRecord._sql.set(query);
        QRecord._session.set(getCurrentSessionInfo());

        try {
            int res = sqlSessionTemplate.insert(id, entity);
            return res;
        } catch (RuntimeException e) {
            System.out.println("Execution failed\n" + query.toString());
            throw e;
        }
    }

    @Override
    public HSchema loadSchema(Class<?> entityType, boolean isEntity) {
        return HSchema.loadSchema(entityType, isEntity, this);
    }

    @Override
    public HSchema getTargetSchema(QJoin join) {
        return join.getTargetRelation(this).loadSchema(this);
    }

    @Override
    public QJoin getJoin(HModel model, String alias) {
        return model.getJoin(alias, this);
    }

    public <E extends QEntity<E>> int update(QUniqueRecord<E> entity) {
        HSchema schema = loadSchema(entity.getClass(), false);
        SqlBuilder gen = new SqlBuilder(schema, this);
        String query = gen.buildUpdate(entity);

        String id = repositoryType.getName() + ".__update__";

        QRecord._sql.set(query);
        QRecord._session.set(getCurrentSessionInfo());

        try {
            int res = sqlSessionTemplate.insert(id, entity);
            return res;
        } catch (RuntimeException e) {
            System.out.println("Execution failed\n" + query.toString());
            throw e;
        }
    }

    public <E extends QEntity<E>> List<E> updateCascadedEntities(Object parentEntityId, QJoin join, List<E> subEntities) {
        HSchema schema = join.getTargetRelation(this).loadSchema(this);
        SqlBuilder gen = new SqlBuilder(schema, this);
        String query = gen.buildUpdateCascaded2(parentEntityId, join, subEntities);

        String id = registerMapper(null, this.schema.getEntityType());


        HFilter._sql.set(query);
        HFilter._session.set(getCurrentSessionInfo());

        try {
            KVEntity param = KVEntity.of("data", subEntities);
            param.put("__sql__", query);
            param.put("parent", KVEntity.of("id", parentEntityId));
            List<E> res = sqlSessionTemplate.selectList(id, param);
            return res;
        } catch (RuntimeException e) {
            System.out.println("Execution failed\n" + query.toString());
            throw e;
        }
    }
    public Object getCurrentSessionInfo() {
        return null;
    }

    private ResultMap createNestedResultMap(Class<?> clazz, String resultMapId, String propertyPrefix) {
        List<ResultMapping> resultMappings = new ArrayList<>();

        for (Field f : clazz.getDeclaredFields()) {
            String columnName = HSchema.Helper.getColumnName(f); // @TColumn 등에서 컬럼명 추출
            if (columnName == null) continue;

            if (HSchema.Helper.isCollectionType(f)) {
                // 1:N Collection 매핑 처리
                Class<?> listItemType = HSchema.Helper.getElementType(f); // List의 제네릭 타입 추출 (예: HpcaTransactionPost)

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
                boolean isPK = HSchema.Helper.isUniqueKey(f);
                ResultMapping mapping = new ResultMapping.Builder(configuration, f.getName(), f.getName(), f.getType())
                        .flags(isPK ? Collections.singletonList(ResultFlag.ID) : Collections.emptyList())
                        .build();
                resultMappings.add(mapping);
            }
        }

        return new ResultMap.Builder(configuration, resultMapId, clazz, resultMappings, true).build();
    }
    
    String registerMapper(XNode sqlNode, Class<?> resultType) {
        String id = repositoryType.getName() + ".__select__." + resultType.getName();

        if (!configuration.hasStatement(id)) {

            String root_id = repositoryType.getName() + ".__select__";
            MappedStatement root_ms = configuration.getMappedStatement(root_id);

            SqlSource sqlSource;
            if (sqlNode == null) {
                sqlSource = root_ms.getSqlSource();
            } else {
                sqlSource = new XMLScriptBuilder(configuration, sqlNode).parseScriptNode();
            }

            ResultMap inlineResultMap = createNestedResultMap(resultType, id + "-Inline", "");
            List<ResultMap> __resultMaps = new ArrayList<>();
            __resultMaps.add(inlineResultMap);

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

    public Object resolveView(String namespace, String sqlFragmentId, Map<String, String> properties) {
        String mapperId = namespace + "." + sqlFragmentId;
        if (configuration.hasStatement(mapperId)) {
            throw new IllegalArgumentException("Only <sql> fragments can be used to create View.");
        }

        XNode fr = configuration.getSqlFragments().get(mapperId);
        if (fr == null) {
            throw new IllegalArgumentException("<sql> fragments is not found. " + mapperId);
        }

        Document doc = fr.getNode().getOwnerDocument();
        Element include = doc.createElement("include");
        include.setAttribute("refid", mapperId);
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            Element property = doc.createElement("property");
            property.setAttribute("name", entry.getKey());
            property.setAttribute("value", entry.getValue());
            include.appendChild(property);
        }
        Element sqlNode = doc.createElement("sql");
        include.setAttribute("id", mapperId + "-sql");
        sqlNode.appendChild(include);

        // <include> 처리.
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration, "???");
        builderAssistant.setCurrentNamespace(namespace);
        XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, builderAssistant);
        includeParser.applyIncludes(sqlNode);
        if (isDynamicXmlQuery(sqlNode)) {
            return sqlNode;
        }

        if (false) {
            // BoundSql bsql = sqlSource.getBoundSql(mapperParams);
            mapperId = registerMapper(fr, /*resultType*/null);
            Object res = sqlSessionTemplate.selectList(mapperId, /*mapperParams*/null);
            //System.out.println(res);
        }

        String sql = fr.getNode().getTextContent();
        sql = new GenericTokenParser("${", "}", properties::get).parse(sql);
        return sql;
    }

    private boolean isDynamicXmlQuery(Node node) {
        switch (node.getNodeName()) {
            case "if": case "choose": case "when": case "foreach": case "bind":
                return true;
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (isDynamicXmlQuery(children.item(i)))
                return true;
        }
        return false;
    }

    @Override
    public <T> T execute(ConnectionCallback<T> action) throws DataAccessException {
        DataSource dataSource = sqlSessionTemplate
                .getSqlSessionFactory()
                .getConfiguration()
                .getEnvironment()
                .getDataSource();

        Connection con = DataSourceUtils.getConnection(dataSource);
        try {
            return action.doInConnection(con);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            DataSourceUtils.releaseConnection(con, dataSource);
        }
    }
}