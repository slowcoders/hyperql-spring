package org.slowcoders.hyperql.sample.hq.bookstore.model;

import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperquery.core.QColumn;
import org.slowcoders.hyperquery.core.QEntity;
import org.slowcoders.hyperquery.core.QFrom;
import org.slowcoders.hyperquery.core.QJoin;

@QFrom("hql_demo.bookstore_jpa.customer_friend_link")
public class CustomerFriendLink implements QEntity<CustomerFriendLink> {
    @Getter
    @Setter
    @QColumn("customer_id")
    private Long customerId;

    @Getter
    @Setter
    @QColumn("friend_id")
    private Long friendId;

    public static QJoin friend = QJoin.of(Customer.class, "#.id = @.friend_id");

    public static QJoin customer = QJoin.of(Customer.class, "#.id = @.customer_id");

}
