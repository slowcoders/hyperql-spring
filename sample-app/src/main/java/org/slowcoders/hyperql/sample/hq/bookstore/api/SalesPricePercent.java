package org.slowcoders.hyperql.sample.hq.bookstore.api;

import lombok.Getter;
import lombok.Setter;
import org.slowcoders.hyperql.sample.hq.bookstore.model.Book;
import org.slowcoders.hyperquery.core.QColumn;
import org.slowcoders.hyperquery.core.QUniqueRecord;
import org.springframework.data.annotation.Transient;

@Getter @Setter
public class SalesPricePercent implements QUniqueRecord<Book> {

    private int id;

    @QColumn(name = "price", writeTransform = "@.price * #{percent}")
    private double price;

    @Transient
    private double percent;
}
