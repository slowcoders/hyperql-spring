package org.slowcoders.basecamp.app.mapper;

import org.slowcoders.basecamp.app.model.UserDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {

    Optional<UserDto> findById(@Param("id") long id);

    List<UserDto> findAll();

    int insert(UserDto user);

    int update(UserDto user);

    int deleteById(@Param("id") long id);

}
