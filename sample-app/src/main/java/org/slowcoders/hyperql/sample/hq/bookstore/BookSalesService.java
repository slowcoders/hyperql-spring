package org.slowcoders.hyperql.sample.hq.bookstore;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.slowcoders.hyperql.sample.hq.bookstore.model.BookSalesRepository;
import org.slowcoders.hyperquery.impl.QStore;
import org.springframework.stereotype.Service;

@Service
public class BookSalesService extends QStore {

    BookSalesService(SqlSessionFactory sqlSessionFactory, SqlSessionTemplate sqlSessionTemplate, BookSalesRepository repository) {
        super(sqlSessionFactory.getConfiguration(), sqlSessionTemplate, repository);
    }

}
