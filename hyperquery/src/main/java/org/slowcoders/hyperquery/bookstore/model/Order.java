package org.slowcoders.hyperquery.bookstore.model;

import org.slowcoders.hyperquery.bookstore.BookDto;
import org.slowcoders.hyperquery.core.Q;
import org.slowcoders.hyperquery.core.QJoin;

@Q.From( "bookstore.books")
public class Order {
    public static QJoin book = QJoin.toMulti(BookDto.class, "#.id = @.book_id");
}
