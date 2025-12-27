package org.slowcoders.hyperql.sample.hq.bookstore.model;

import org.apache.ibatis.annotations.Mapper;
import org.slowcoders.hyperquery.core.Q;
import org.slowcoders.hyperquery.core.QJoin;
import org.slowcoders.hyperquery.core.QRelation;

@Mapper
@Q.From("bookstore.book")
public interface BookRepository extends QRelation {
    QJoin author = QJoin.toSingle(AuthorRepository.class, "#.id = @.author_id");
}
