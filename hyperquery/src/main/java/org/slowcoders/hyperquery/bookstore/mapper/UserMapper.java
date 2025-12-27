package org.slowcoders.hyperquery.bookstore.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.slowcoders.hyperquery.bookstore.UserDto;

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
