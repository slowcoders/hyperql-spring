package org.slowcoders.hyperql.sample.hq.bookstore.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperquery.core.QEntity;

import java.util.Set;


public class Customer implements QEntity<Customer> {
    @Getter
    @Setter
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Getter @Setter
    @Column(name = "name", nullable = false)
    private String name;

    @Getter @Setter
    @Column(name = "height", nullable = true)
    private Float height;

    @Getter @Setter
    @Column(name = "mass", nullable = true)
    private Float mass;

//    @Getter @Setter
//    @Column(name = "memo", nullable = true, columnDefinition = "jsonb")
//    @org.hibernate.annotations.Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)
//    private com.fasterxml.jackson.databind.JsonNode memo;

    @Getter @Setter
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "customer_friend_link", schema = "bookstore_jpa", catalog = "bookstore_jpa",
            uniqueConstraints = {
                    @UniqueConstraint(name ="customer_id__friend_id__uindex", columnNames = {"customer_id", "friend_id"})
            },
            joinColumns = @JoinColumn(name="customer_id"), inverseJoinColumns = @JoinColumn(name="friend_id"))
    private Set<Customer> friend_;

    @Getter @Setter
    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(name = "book_order", schema = "bookstore_jpa", catalog = "bookstore_jpa", 
        uniqueConstraints = {
                @UniqueConstraint(name = "customer_id__book_id__uindex", columnNames ={"customer_id", "book_id"})
        },
        joinColumns = @JoinColumn(name = "customer_id"), inverseJoinColumns = @JoinColumn(name = "book_id"))
    private Set<Book>book_;
}
