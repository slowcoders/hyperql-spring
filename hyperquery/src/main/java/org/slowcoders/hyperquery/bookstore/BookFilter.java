package org.slowcoders.hyperquery.bookstore;

import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperquery.bookstore.model.Book;
import org.slowcoders.hyperquery.core.QFilter;

@Getter
@Setter
public class BookFilter extends QFilter<Book> {
    @Condition("tile ilike '%' || ? || '%")
    private String title;

    BookFilter() {
        super(Book.class);
    }
}
