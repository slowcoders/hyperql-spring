package org.slowcoders.hyperql.sample.session;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.slowcoders.basecamp.security.SessionInfo;
import org.slowcoders.hyperql.sample.hq.bookstore.api.HqBookController;
import org.slowcoders.hyperql.sample.hq.bookstore.model.BookSales;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {

    Optional<UserDto> findById(@Param("userId") String id);

    List<UserDto> findAll();

    int insert(UserDto user);

    int update(UserDto user);

    int deleteById(@Param("id") long id);

    List<BookSales> getBookSales(HqBookController.BookSalesFilter salesFilter);

}
