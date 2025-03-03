package org.slowcoders.hyperql.sample.jdbc.bookstore.controller;

import jakarta.annotation.PostConstruct;
import org.slowcoders.hyperql.HyperStorageController;
import org.slowcoders.hyperql.schema.EntityAccessGuard;
import org.slowcoders.hyperql.schema.EntityAccessType;
import org.slowcoders.hyperql.jdbc.JdbcStorage;
import org.slowcoders.hyperql.jdbc.storage.JdbcColumn;
import org.slowcoders.hyperql.jdbc.storage.JdbcSchema;
import org.slowcoders.hyperql.schema.QColumn;
import org.slowcoders.hyperql.schema.QSchema;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hql/bookstore")
public class BookStoreController extends HyperStorageController.CRUD implements HyperStorageController.ListAll {

    public BookStoreController(JdbcStorage storage, ConversionService conversionService) {
        super(storage, "bookstore", conversionService);
    }

    @PostConstruct
    void init() {
        var storage = (JdbcStorage)this.getStorage();
        storage.setSortCollation(" COLLATE \"ko_KR.utf8\"");
        {
            JdbcColumn fkCol = (JdbcColumn) this.getStorage().loadSchema("bookstore.book").getColumn("publisher_id");
            fkCol.setJoinedPrimaryColumn_unsafe("publisher", "bookstore.publisher", "id");
        }
        {
            storage.addVirtualTable("bookstore.best_seller", best_seller_filter, null, new String[]{"id"});
            JdbcColumn fkCol = (JdbcColumn) this.getStorage().loadSchema("bookstore.best_seller").getColumn("author_id");
            fkCol.setJoinedPrimaryColumn_unsafe("author", "bookstore.author", "id");
        }
        {
            JdbcSchema schema = (JdbcSchema) this.getStorage().loadSchema("bookstore.customer");
            schema.setAccessGuard(new EntityAccessGuard() {
                @Override
                public String checkReadable(QSchema schema, String entityAlias, List<QColumn> columns) throws SecurityException {
                    return "true and true";
                }
            });
        }
    }

    private static final String best_seller_filter = """
        select * from bookstore.book bk
        left join (select book_id, count(*) as count 
                    from bookstore.book_order 
                    group by book_id
            ) orders on orders.book_id = bk.id
    """;

}
