package org.slowcoders.hyperql.sample.hq.bookstore;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.slowcoders.hyperql.sample.hq.bookstore.model.BookRepository;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Book;
import org.slowcoders.hyperquery.impl.QStore;
import org.springframework.stereotype.Service;

@Service
public class BookService extends QStore<Book> {

    BookService(SqlSessionFactory sqlSessionFactory, SqlSessionTemplate sqlSessionTemplate, BookRepository repository) {
        super(sqlSessionFactory.getConfiguration(), sqlSessionTemplate, BookRepository.class);
    }

}
