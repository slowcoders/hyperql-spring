package org.slowcoders.hyperql.sample.hq.bookstore.model;

import org.apache.ibatis.annotations.Mapper;
import org.slowcoders.hyperquery.impl.QRepository;

@Mapper
public interface AuthorRepository extends QRepository<Author> {
}
