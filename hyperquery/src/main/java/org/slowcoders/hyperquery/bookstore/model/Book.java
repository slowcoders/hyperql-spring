package org.slowcoders.hyperquery.bookstore.model;

import org.slowcoders.hyperquery.core.Q;
import org.slowcoders.hyperquery.core.QJoin;

@Q.From("bookstore.book")
public interface Book {
    QJoin author = QJoin.toSingle(Author.class, "#.id = @.author_id");

    QJoin orders = QJoin.toMulti(Order.class, "#.book_id = @.book_id");
}
