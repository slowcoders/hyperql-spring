package org.slowcoders.hyperql.sample.hq.bookstore.api;

import org.slowcoders.hyperql.sample.hq.bookstore.AuthorDto;
import org.slowcoders.hyperql.sample.hq.bookstore.AuthorFilter;
import org.slowcoders.hyperql.sample.hq.bookstore.AuthorService;
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
    public List<AuthorDto> search(@RequestBody AuthorFilter filter) {
        List<AuthorDto> res = service.selectList(AuthorDto.class, filter);
        return res;
    }

    @GetMapping("/{id}")
    public List<AuthorDto> get(@PathVariable("id") String id) {
        return service.selectList(AuthorDto.class, null);
    }
}
