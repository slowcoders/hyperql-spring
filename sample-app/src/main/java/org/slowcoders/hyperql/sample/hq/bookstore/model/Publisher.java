package org.slowcoders.hyperql.sample.hq.bookstore.model;

import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperquery.core.QColumn;
import org.slowcoders.hyperquery.core.QEntity;
import org.slowcoders.hyperquery.core.QFrom;
import org.slowcoders.hyperquery.core.QJoin;

@QFrom("hql_demo.bookstore.publisher")
public class Publisher implements QEntity<Publisher> {
    @Getter
    @Setter
    private Long id;

    @Getter
    @Setter
    private com.fasterxml.jackson.databind.JsonNode memo;

    @Getter
    @Setter
    private String name;

    public static QJoin book_ = QJoin.of(Book.class, "#.publisher_id = @.id");

}
