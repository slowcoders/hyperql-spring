package org.slowcoders.hyperql.sample.hq.bookstore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperquery.core.QEntity;
import org.slowcoders.hyperquery.core.QFrom;
import org.slowcoders.hyperquery.impl.QAttribute;

import java.util.List;
import java.util.Set;


@Getter
@Setter
@QFrom("bookstore.author")
public class Author implements QEntity<Author> {

    static final Join books = Join.toMany(Book.class, "#.author_id = @.id");
    static final QAttribute bookCount = Property.formula("""
            select count(*) from bookstore.book bk where bk.author_id = @.id and true
            """);

    static final QAttribute bookPriceAvr = Property.formula("""
            select sum(price) * @.bookCount from bookstore.book bk where bk.author_id = @.id and true
            """);
    static final QAttribute salesAmount = Property.formula("""
            @.bookCount * @.bookPriceAvr
            """);

    @Id
    @Column(name = "id", nullable = false)
    @PKColumn("id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

//    @Getter @Setter
//    @Column(name = "profile", nullable = true, columnDefinition = "jsonb")
//    @org.hibernate.annotations.Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)
//    private com.fasterxml.jackson.databind.JsonNode profile;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "author")
    @Column(name = "@books", nullable = false)
    private List<Book> book_;
}
