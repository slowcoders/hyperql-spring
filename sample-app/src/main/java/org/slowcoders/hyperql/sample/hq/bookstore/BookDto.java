package org.slowcoders.hyperql.sample.hq.bookstore;

import lombok.Getter;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Book;
import org.slowcoders.hyperquery.core.QColumn;
import org.slowcoders.hyperquery.core.QRecord;

import java.time.OffsetDateTime;

@Getter
public class BookDto implements QRecord<Book> {

    private Long id;

    private String title;

    @QColumn("@author_.name")
    private String author;

    private OffsetDateTime createdAt;

}



