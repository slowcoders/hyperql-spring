package org.slowcoders.hyperql.sample.hq.bookstore;

import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Author;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Book;
import org.slowcoders.hyperquery.core.QFilter;

@Getter
@Setter
public class AuthorFilter extends QFilter<Author> {
    @Predicate("@.name ilike '%' || ? || '%'")
    private String name;

    AuthorFilter() {
        super(Author.class);
    }
}
