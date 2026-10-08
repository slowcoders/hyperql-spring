package org.slowcoders.hyperql.sample.hq.bookstore;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Book;
import org.slowcoders.hyperquery.core.QColumn;
import org.slowcoders.hyperquery.core.QFilter;
import org.slowcoders.hyperquery.core.QRecord;

import java.time.LocalDate;

@Getter
@ToString
public class BookDto implements QRecord<Book> {

    private Long id;

    private String title;

    @QColumn(name = "@author.name")
    private String author;

//    @QColumn("@weeklySales")
//    private List<HqBookController.BookSalesFilter> weeklySales;

    @Getter @Setter
    public static class Filter extends QFilter<Book> {
        private LocalDate startDate;
        private LocalDate endDate;
        @Predicate("@.id = ?")
        private int id;
    }
}



