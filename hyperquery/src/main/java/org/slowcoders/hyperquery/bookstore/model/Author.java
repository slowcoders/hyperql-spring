package org.slowcoders.hyperquery.bookstore.model;

import org.slowcoders.hyperquery.core.Q;
import org.slowcoders.hyperquery.core.QJoin;

@Q.From("bookstore.author")
public interface Author {
    QJoin books = QJoin.toMulti(Book.class, "#.author_id = @.id");
}
