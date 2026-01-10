package org.slowcoders.hyperql.sample.hq.bookstore;

import lombok.Getter;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Author;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Book;
import org.slowcoders.hyperquery.core.QColumn;
import org.slowcoders.hyperquery.core.QRecord;

import java.util.List;

@Getter
public class AuthorDto implements QRecord<Author> {

    @QColumn("id")
    private Long id;

    @QColumn("name")
    private String name;

    @QColumn("@books")
    private List<Book> books;

    @QColumn("bookCount")
    private int bookCount;

    @QColumn("salesAmount")
    private int salesAmount;

    @QColumn("bookPriceAvr")
    private int bookPriceAvr;

//    @QColumn("attr2")
//    private int attr2;
}



