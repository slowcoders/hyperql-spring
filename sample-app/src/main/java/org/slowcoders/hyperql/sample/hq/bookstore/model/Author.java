package org.slowcoders.hyperql.sample.hq.bookstore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperquery.core.QEntity;
import org.slowcoders.hyperquery.core.QFrom;
import org.slowcoders.hyperquery.core.QRepository;

import java.util.Set;


@QFrom("bookstore.author")
public class Author implements QEntity {

    static final Join books = Join.toMany(Book.class, "#.author_id = @.id");

    @Getter
    @Setter
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Getter @Setter
    @Column(name = "name", nullable = false)
    private String name;

    @Getter @Setter
    @Column(name = "profile", nullable = true, columnDefinition = "jsonb")
    @org.hibernate.annotations.Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)
    private com.fasterxml.jackson.databind.JsonNode profile;

    @Getter @Setter
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "author")
    private Set<Book> book_;
}
