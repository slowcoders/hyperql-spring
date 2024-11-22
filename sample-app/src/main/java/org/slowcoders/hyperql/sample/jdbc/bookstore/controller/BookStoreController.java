package org.slowcoders.hyperql.sample.jdbc.bookstore.controller;

import jakarta.annotation.PostConstruct;
import org.slowcoders.hyperql.HyperStorage;
import org.slowcoders.hyperql.HyperStorageController;
import org.slowcoders.hyperql.jdbc.JdbcStorage;
import org.slowcoders.hyperql.jdbc.storage.JdbcColumn;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hql/bookstore")
public class BookStoreController extends HyperStorageController.CRUD implements HyperStorageController.ListAll {

    public BookStoreController(JdbcStorage storage, ConversionService conversionService) {
        super(storage, "bookstore", conversionService);
    }

    @PostConstruct
    void init() {
        var storage = (JdbcStorage)this.getStorage();
        storage.addVirtualTable("bookstore.best_seller", best_seller_filter, null, new String[] {"id"});
        JdbcColumn fkCol = (JdbcColumn) this.getStorage().loadSchema("bookstore.best_seller").getColumn("author_id");
        fkCol.setJoinedPrimaryColumn_unsafe("author", "bookstore.author", "id");
    }

    private static final String best_seller_filter = """
        select * from bookstore.book bk
        left join (select book_id, count(*) as count 
                    from bookstore.book_order 
                    group by book_id
            ) orders on orders.book_id = bk.id
    """;

}
