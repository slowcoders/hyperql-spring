package org.slowcoders.hyperql.sample.hq.bookstore.model;

import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperquery.core.QColumn;
import org.slowcoders.hyperquery.core.QEntity;
import org.slowcoders.hyperquery.core.QFrom;
import org.slowcoders.hyperquery.core.QJoin;

@QFrom("hql_demo.bookstore_jpa.publisher")
public class Publisher implements QEntity<Publisher> {
    @Getter
    @Setter
    @QColumn("id")
    private Long id;

    @Getter
    @Setter
    @QColumn("memo")
    private com.fasterxml.jackson.databind.JsonNode memo;

    @Getter
    @Setter
    @QColumn("name")
    private String name;

    public static QJoin book_ = QJoin.of(Book.class, "#.publisher_id = @.id");

}
