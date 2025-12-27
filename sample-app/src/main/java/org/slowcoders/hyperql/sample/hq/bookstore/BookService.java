package org.slowcoders.hyperql.sample.hq.bookstore;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.slowcoders.hyperquery.QStore;
import org.springframework.stereotype.Service;

@Service
public class BookService extends QStore {
    BookService(SqlSessionFactory sqlSessionFactory, SqlSessionTemplate sqlSessionTemplate) {
        super(sqlSessionFactory, sqlSessionTemplate);
    }

}
