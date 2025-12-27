package org.slowcoders.hyperquery.bookstore.model;

import org.apache.ibatis.annotations.Mapper;
import org.slowcoders.hyperquery.bookstore.BookDto;
import org.slowcoders.hyperquery.core.Q;
import org.slowcoders.hyperquery.core.QJoin;

@Q.From("bookstore.user")
@Mapper
public interface User {
    QJoin book = QJoin.toMulti(BookDto.class, "#.id = @.book_id");

}
