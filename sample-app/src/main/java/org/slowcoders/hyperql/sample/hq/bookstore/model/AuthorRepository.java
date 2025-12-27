package org.slowcoders.hyperql.sample.hq.bookstore.model;

import org.apache.ibatis.annotations.Mapper;
import org.slowcoders.hyperquery.core.Q;
import org.slowcoders.hyperquery.core.QJoin;
import org.slowcoders.hyperquery.core.QRelation;

@Mapper
@Q.From("bookstore.author")
public interface AuthorRepository extends QRelation {
    QJoin books = QJoin.toMulti(BookRepository.class, "#.author_id = @.id");
}
