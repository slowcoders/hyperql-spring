package org.slowcoders.hyperquery;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.slowcoders.hyperquery.core.QFilter;
import org.slowcoders.hyperquery.mybatis.QueryBuilder2;

import java.util.List;

public class QStore {
    private final Configuration configuration;
    private final SqlSessionTemplate sqlSessionTemplate;

    private final QueryBuilder2 qb;

    public QStore(SqlSessionFactory sqlSessionFactory, SqlSessionTemplate sqlSessionTemplate) {
        this.configuration = sqlSessionFactory.getConfiguration();
        this.sqlSessionTemplate = sqlSessionTemplate;
        qb = new QueryBuilder2(configuration, sqlSessionTemplate);
    }

    public <T> List<T> selectList(Class<T> recordType, QFilter<?> filter) {
        return qb.selectEntities(recordType, filter);
    }

    public <T> T selectOne(Class<T> recordType, QFilter<?> filter) {
        return qb.selectOne(recordType, filter);
    }
}
