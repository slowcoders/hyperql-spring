package org.slowcoders.hyperql.sample.hq.bookstore;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Author;
import org.slowcoders.hyperql.sample.hq.bookstore.model.BookOrderRepository;
import org.slowcoders.hyperquery.impl.QStore;
import org.springframework.stereotype.Service;

@Service
public class BookOrderService extends QStore<Author> {
    BookOrderService(SqlSessionFactory sqlSessionFactory, SqlSessionTemplate sqlSessionTemplate, BookOrderRepository repository) {
        super(sqlSessionFactory.getConfiguration(), sqlSessionTemplate, repository);
    }
}
