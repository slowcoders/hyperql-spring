package org.slowcoders.hyperql.sample.hpms.api;

import org.slowcoders.hyperql.sample.hq.bookstore.UserDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController( "/api/users")
public class UserController {
    @GetMapping("/")
    public List<UserDto> index() {
        return null;//"Hello World!";
    }
}
