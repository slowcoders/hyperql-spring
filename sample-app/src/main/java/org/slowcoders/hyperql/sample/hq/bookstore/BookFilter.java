package org.slowcoders.hyperql.sample.hq.bookstore;

import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Book;
import org.slowcoders.hyperquery.core.QFilter;

@Getter
@Setter
public class BookFilter extends QFilter<Book> {
    @Predicate("title ilike '%' || ? || '%'")
    private String title;

    BookFilter() {
        super(Book.class);
    }
}
