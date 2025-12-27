package org.slowcoders.hyperql.sample.hq.bookstore;

import lombok.Getter;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Book;
import org.slowcoders.hyperql.sample.hq.bookstore.model.BookRepository;
import org.slowcoders.hyperquery.core.QColumn;
import org.slowcoders.hyperquery.core.QRecord;

import java.time.OffsetDateTime;

@Getter
public class BookDto extends QRecord<BookRepository> {

    @QColumn("id")
    private Long id;

    @QColumn("title")
    private String title;

    @QColumn("@author.name")
    private String author;

    private OffsetDateTime createdAt;

    public BookDto() {
        super(BookRepository.class);
    }

}



