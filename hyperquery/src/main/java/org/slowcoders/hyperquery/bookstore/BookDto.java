package org.slowcoders.hyperquery.bookstore;

import lombok.Getter;
import org.slowcoders.hyperquery.bookstore.model.Book;
import org.slowcoders.hyperquery.core.QRecord;

import java.time.OffsetDateTime;

@Getter
public class BookDto extends QRecord<Book> {

    private Long id;
    private String title;
    private String name;
    private OffsetDateTime createdAt;

    public BookDto() {
        super(Book.class);
    }

}



