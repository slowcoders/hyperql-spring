package org.slowcoders.hyperql.sample.hq.bookstore;

import lombok.Getter;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Author;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Book;
import org.slowcoders.hyperquery.core.QColumn;
import org.slowcoders.hyperquery.core.QRecord;

import java.util.List;

@Getter
public class AuthorDto implements QRecord<Author> {

    private Long id;

    private String name;

    private List<Book> book_;

//    private int bookCount;
//
//    private int salesAmount;
//
//    private int bookPriceAvr;

//    @QColumn("attr2")
//    private int attr2;
}



