package org.slowcoders.hyperql.sample.hq.bookstore.api;

import org.slowcoders.hyperql.sample.hq.bookstore.BookDto;
import org.slowcoders.hyperql.sample.hq.bookstore.BookFilter;
import org.slowcoders.hyperql.sample.hq.bookstore.BookService;
import org.slowcoders.hyperql.sample.hq.bookstore.mapper.UserMapper;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Book;
import org.slowcoders.hyperql.sample.hq.bookstore.model.BookSales;
import org.slowcoders.hyperquery.core.QFilter;
import org.slowcoders.hyperquery.core.QMapperView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/hq/books")
public class HqBookController {

    private final BookService service;
    private final UserMapper userMapper;

    HqBookController(BookService service, UserMapper userMapper) {
        this.userMapper = userMapper;
        this.service = service;
    }

    @GetMapping("/")
    public List<Book> search(BookFilter filter) {
        return service.selectList(Book.class, filter);
    }

    @GetMapping("/{id}")
    public List<BookDto> get(@PathVariable("id") int id) {
        BookDto.Filter filter = new BookDto.Filter();
        filter.setId(id);
        filter.setStartDate(LocalDate.of(1970, 1, 1));
        filter.setEndDate(LocalDate.of(2970, 1, 1));
        List<BookDto> res = service.selectList(BookDto.class, filter);
        System.out.println(res);
        return res;
    }

    @GetMapping("/sales")
    public List<BookSales> getSalesSummary(BookSalesFilter salesFilter) {
        List<BookSales> res1 = userMapper.getBookSales(salesFilter);
        QMapperView<BookSales> salesView = BookSales.summaries("#{startDate}", "#{endDate}");
        List<BookSales> res2 = service.selectList(salesView, BookSales.class, salesFilter);
        System.out.println(res1);
        System.out.println(res2);
        return res2;
    }

    public static class BookSalesFilter extends QFilter<BookSales> {
        LocalDate startDate;
        LocalDate endDate;

        Integer bookId;
    }
}
