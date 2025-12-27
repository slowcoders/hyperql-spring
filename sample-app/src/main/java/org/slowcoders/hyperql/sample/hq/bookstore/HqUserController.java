package org.slowcoders.hyperql.sample.hq.bookstore;

import lombok.RequiredArgsConstructor;
import org.slowcoders.hyperql.sample.hq.bookstore.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/hq/users")
@RequiredArgsConstructor
public class HqUserController {

    private final UserMapper userMapper;

    @GetMapping
    public List<UserDto> findAll() {
        return userMapper.findAll();
    }

    @GetMapping("/{id}")
    public UserDto findById(@PathVariable long id) {
        return userMapper.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@RequestBody UserDto body) {
        if (body.getId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id must be null on create");
        }
        userMapper.insert(body); // useGeneratedKeys 로 id 채워지는 전제
        return body;
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable long id, @RequestBody UserDto body) {
        body.setId(id);
        int updated = userMapper.update(body);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id);
        }
        return userMapper.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        int deleted = userMapper.deleteById(id);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id);
        }
    }
}
