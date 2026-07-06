package org.slowcoders.hyperql.sample.hq.bookstore.api;

import org.slowcoders.hyperql.sample.hq.bookstore.BookOrderService;
import org.slowcoders.hyperql.sample.session.UserMapper;
import org.slowcoders.hyperql.sample.hq.bookstore.model.BookOrder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hq/orders")
public class HqBookOrderController {

    private final BookOrderService service;
    private final UserMapper userMapper;

    HqBookOrderController(BookOrderService service, UserMapper userMapper) {
        this.userMapper = userMapper;
        this.service = service;
    }

//    @GetMapping("/")
//    public List<BookOrder> search(@RequestBody BookOrderFilter filter) {
//        return service.selectList(BookOrder.class, filter);
//    }

    @PostMapping("/")
    public int insert(@RequestBody BookOrder book) {
        return service.insert(book, true);
    }


//    @GetMapping("/book/{id}")
//    public List<BookOrder> get(@PathVariable("id") int bookId) {
//        List<BookOrder> res = service.selectList(BookOrder.class, filter);
//        System.out.println(res);
//        return res;
//    }

    @PostMapping("/book/{id}/")
    public List<BookOrder> updateOrderList(@PathVariable("id") int bookId, @RequestBody List<BookOrder> orders) {
        List<BookOrder> new_orders = service.updateCascadedEntities(bookId, BookOrder.book, orders);
        return new_orders;
    }

}
