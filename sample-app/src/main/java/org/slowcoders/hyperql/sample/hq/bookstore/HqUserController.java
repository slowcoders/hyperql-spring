package org.slowcoders.hyperql.sample.hq.bookstore;

import lombok.RequiredArgsConstructor;
import org.slowcoders.basecamp.security.SecurityUtil;
import org.slowcoders.hyperql.sample.session.UserDto;
import org.slowcoders.hyperql.sample.session.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/hq/users")
@RequiredArgsConstructor
public class HqUserController {

    private final UserMapper userMapper;
    private final SecurityUtil securityUtil;

    @GetMapping
    public List<UserDto> findAll() {
        return userMapper.findAll();
    }

    @GetMapping("/{id}")
    public UserDto findById(@PathVariable String id) {
        return userMapper.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@RequestBody UserDto body) {
        if (body.getPassword() != null) {
            body.setPassword(securityUtil.encrypt(body.getPassword()));
        }
        userMapper.insert(body); // useGeneratedKeys 로 id 채워지는 전제
        return body;
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable String id, @RequestBody UserDto body) {
        body.setUserId(id);
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
