package org.slowcoders.hyperql.sample.hq.bookstore.api;

import org.slowcoders.hyperql.sample.hq.bookstore.AuthorDto;
import org.slowcoders.hyperql.sample.hq.bookstore.AuthorFilter;
import org.slowcoders.hyperql.sample.hq.bookstore.AuthorService;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Author;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hq/authors")
public class HqAuthorController {

    private final AuthorService service;

    HqAuthorController(AuthorService service) {
        this.service = service;
    }

    @PostMapping("/")
    public List<Author> search(@RequestBody AuthorFilter filter) {
        List<Author> res = service.selectList(Author.class, filter);
        return res;
    }

    @GetMapping("/{id}")
    public List<AuthorDto> get(@PathVariable("id") String id) {
        return service.selectList(AuthorDto.class, null);
    }
}
