package org.slowcoders.hyperql.sample.hq.bookstore.model;

import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperquery.core.QColumn;
import org.slowcoders.hyperquery.core.QEntity;
import org.slowcoders.hyperquery.core.QFrom;
import org.slowcoders.hyperquery.core.QJoin;

@QFrom("hql_demo.bookstore.book_order")
public class BookOrder implements QEntity<BookOrder> {
    @Getter
    @Setter
    @QColumn("customer_id")
    private Long customerId;

    @Getter
    @Setter
    @QColumn("book_id")
    private Long bookId;

    public static QJoin book = QJoin.of(Book.class, "#.id = @.book_id");

    public static QJoin customer = QJoin.of(Customer.class, "#.id = @.customer_id");

}
