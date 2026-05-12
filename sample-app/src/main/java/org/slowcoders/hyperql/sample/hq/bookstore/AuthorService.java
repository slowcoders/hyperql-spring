package org.slowcoders.hyperql.sample.hq.bookstore;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.slowcoders.hyperql.sample.hq.bookstore.model.AuthorRepository;
import org.slowcoders.hyperquery.impl.QStore;
import org.springframework.stereotype.Service;

@Service
public class AuthorService extends QStore {
    AuthorService(SqlSessionFactory sqlSessionFactory, SqlSessionTemplate sqlSessionTemplate, AuthorRepository repository) {
        super(sqlSessionFactory.getConfiguration(), sqlSessionTemplate, repository);
    }
}
