package org.slowcoders.hyperql.sample.hq.bookstore.api;

import org.slowcoders.hyperql.sample.hq.bookstore.BookDto;
import org.slowcoders.hyperql.sample.hq.bookstore.BookFilter;
import org.slowcoders.hyperql.sample.hq.bookstore.BookService;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Book;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hq/books")
public class HqBookController {

    private final BookService service;

    HqBookController(BookService service) {
        this.service = service;
    }

    @GetMapping("/")
    public List<Book> search(BookFilter filter) {
        return service.selectList(Book.class, filter);
    }

    @GetMapping("/{id}")
    public List<BookDto> get(@PathVariable("id") String id) {
        return service.selectList(BookDto.class, null);
    }
}
