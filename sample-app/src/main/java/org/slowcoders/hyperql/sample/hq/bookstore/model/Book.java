package org.slowcoders.hyperql.sample.hq.bookstore.model;

import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperquery.core.QColumn;
import org.slowcoders.hyperquery.core.QEntity;
import org.slowcoders.hyperquery.core.QFrom;
import org.slowcoders.hyperquery.core.QJoin;

@QFrom("hql_demo.bookstore.book")
public class Book implements QEntity<Book> {
    @Getter
    @Setter
    @QColumn("id")
    private Long id;

    @Getter
    @Setter
    @QColumn("price")
    private Float price;

    @Getter
    @Setter
    @QColumn("title")
    private String title;

    @Getter
    @Setter
    @QColumn("author_id")
    private Long authorId;

    @Getter
    @Setter
    @QColumn("publisher_id")
    private Long publisherId;

    public static QJoin bookOrder_ = QJoin.of(BookOrder.class, "#.book_id = @.id");

    public static QJoin publisher = QJoin.of(Publisher.class, "#.id = @.publisher_id");

    public static QJoin author = QJoin.of(Author.class, "#.id = @.author_id");

    public static QJoin customer_ = QJoin.of(Customer.class, "#.book_id = @.id");

}
