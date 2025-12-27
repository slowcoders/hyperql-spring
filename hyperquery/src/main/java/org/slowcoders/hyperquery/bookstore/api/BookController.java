package org.slowcoders.hyperquery.bookstore.api;

import org.slowcoders.hyperquery.bookstore.BookDto;
import org.slowcoders.hyperquery.bookstore.BookFilter;
import org.slowcoders.hyperquery.bookstore.model.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("/api/books")
public class BookController {

    private final BookService service;

    BookController(BookService service) {
        this.service = service;
    }

    @GetMapping("/")
    public List<BookDto> search(BookFilter filter) {
        return service.selectList(BookDto.class, filter);
    }

    @GetMapping("/{id}")
    public List<BookDto> get(@PathVariable("id") String id) {
        return service.selectList(BookDto.class, null);
    }
}
