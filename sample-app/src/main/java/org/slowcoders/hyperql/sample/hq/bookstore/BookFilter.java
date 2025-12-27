package org.slowcoders.hyperql.sample.hq.bookstore;

import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Book;
import org.slowcoders.hyperql.sample.hq.bookstore.model.BookRepository;
import org.slowcoders.hyperquery.core.QFilter;

@Getter
@Setter
public class BookFilter extends QFilter<BookRepository> {
    @Condition("title ilike '%' || ? || '%'")
    private String title;

    BookFilter() {
        super(BookRepository.class);
    }
}
