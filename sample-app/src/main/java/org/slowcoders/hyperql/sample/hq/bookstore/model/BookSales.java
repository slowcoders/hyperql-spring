package org.slowcoders.hyperql.sample.hq.bookstore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.ToString;
import org.slowcoders.hyperql.sample.hq.bookstore.mapper.UserMapper;
import org.slowcoders.hyperquery.core.*;

import java.util.Map;


@Getter
@ToString
public class BookSales implements QEntity<BookSales> {

    public static QMapperView<BookSales> summaries(String startDate, String endDate) {
        return new QMapperView<>(BookSales.class, UserMapper.class, "bookSales", Map.of(
                "startDate", startDate,
                "endDate", endDate
        ));

    }

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
