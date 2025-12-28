package org.slowcoders.hyperql.sample.hq.bookstore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperquery.core.QEntity;
import org.slowcoders.hyperquery.core.QFrom;
import org.slowcoders.hyperquery.core.QRepository;

import java.util.Set;


@QFrom("bookstore.book")
public class Book implements QEntity {

    static final Join author_ = Join.toOne(Author.class, "#.id = @.author_id");

    @Getter @Setter
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Getter @Setter
    @Column(name = "title", nullable = false)
    private String title;

    @Getter @Setter
    @Column(name = "price", nullable = true)
    private Float price;

    @Getter @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id", nullable = true, referencedColumnName = "id")
    private Publisher publisher;

    @Getter @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = true, referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_author_id_2_pk_author__id"))
    private Author author;

    @Getter @Setter
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "book_order", schema = "bookstore_jpa", catalog = "bookstore_jpa",
            uniqueConstraints = {
                    @UniqueConstraint(name ="customer_id__book_id__uindex", columnNames = {"customer_id", "book_id"})
            },
            joinColumns = @JoinColumn(name="book_id"), inverseJoinColumns = @JoinColumn(name="customer_id"))
    private Set<Customer> customer_;

}
