package org.slowcoders.hyperql.sample.hq.bookstore.model;

import jakarta.persistence.*;
import lombok.Getter;
import org.slowcoders.hyperql.sample.hq.bookstore.mapper.UserMapper;
import org.slowcoders.hyperquery.core.*;


@Getter
@QFromMapper(mapper = UserMapper.class, sqlId = "bookSales")
@QFilterParameters({"startDate", "endDate"})
public class BookSales implements QEntity<BookSales> {

    @Getter
    @Column(name = "ranking")
    private int ranking;

    @Getter
    @Column(name = "book_id")
    private int bookId;

    @Getter
    @Column(name = "title")
    private String title;

//    @Getter
//    @Column(name = "price")
//    private Float price;

    @Getter
    @Column(name = "count")
    private Float count;

    @Getter
    @Column(name = "amount")
    private Float amount;

}
