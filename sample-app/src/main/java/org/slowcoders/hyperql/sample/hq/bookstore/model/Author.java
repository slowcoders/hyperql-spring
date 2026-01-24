package org.slowcoders.hyperql.sample.hq.bookstore.model;

import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperquery.core.QColumn;
import org.slowcoders.hyperquery.core.QEntity;
import org.slowcoders.hyperquery.core.QFrom;
import org.slowcoders.hyperquery.core.QJoin;

@QFrom("hql_demo.bookstore_jpa.author")
public class Author implements QEntity<Author> {
    @Getter
    @Setter
    @QColumn("id")
    private Long id;

    @Getter
    @Setter
    @QColumn("name")
    private String name;

    @Getter
    @Setter
    @QColumn("profile")
    private com.fasterxml.jackson.databind.JsonNode profile;

    public static QJoin book_ = QJoin.of(Book.class, "#.author_id = @.id");

}
