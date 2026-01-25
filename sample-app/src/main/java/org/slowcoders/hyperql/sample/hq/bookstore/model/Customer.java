package org.slowcoders.hyperql.sample.hq.bookstore.model;

import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperquery.core.QColumn;
import org.slowcoders.hyperquery.core.QEntity;
import org.slowcoders.hyperquery.core.QFrom;
import org.slowcoders.hyperquery.core.QJoin;

@QFrom("hql_demo.bookstore.customer")
public class Customer implements QEntity<Customer> {
    @Getter
    @Setter
    @QColumn("id")
    private Long id;

    @Getter
    @Setter
    @QColumn("height")
    private Float height;

    @Getter
    @Setter
    @QColumn("mass")
    private Float mass;

    @Getter
    @Setter
    @QColumn("memo")
    private com.fasterxml.jackson.databind.JsonNode memo;

    @Getter
    @Setter
    @QColumn("name")
    private String name;

    public static QJoin bookOrder_ = QJoin.of(BookOrder.class, "#.customer_id = @.id");

    public static QJoin customerFriendLink_ = QJoin.of(CustomerFriendLink.class, "#.customer_id = @.id");

    public static QJoin friend_ = QJoin.of(Customer.class, "#.customer_id = @.id");

    public static QJoin book_ = QJoin.of(Book.class, "#.customer_id = @.id");

}
