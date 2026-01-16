package org.slowcoders.hyperql.sample.hq.bookstore;

import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Book;
import org.slowcoders.hyperql.sample.hq.bookstore.model.BookSales;
import org.slowcoders.hyperquery.core.QColumn;
import org.slowcoders.hyperquery.core.QFilter;
import org.slowcoders.hyperquery.core.QRecord;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
public class BookDto implements QRecord<Book> {

    private Long id;

    private String title;

    @QColumn("@author_.name")
    private String author;

    @QColumn("@weeklySales")
    private List<BookSales> weeklySales;

    @Getter @Setter
    public static class Filter extends QFilter<Book> {
        private LocalDate startDate;
        private LocalDate endDate;
        @Predicate("@.id = ?")
        private int id;
    }
}



