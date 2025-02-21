//import io.swagger.v3.oas.annotations.media.Schema;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.slowcoders.hyperql.HyperStorageController;
//import org.slowcoders.hyperql.OutputOptions;
//import org.slowcoders.hyperql.sample.jpa.bookstore_jpa.service.BookStoreJpaService;
//import org.springframework.core.convert.ConversionService;
//import java.util.Map;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//
//package org.slowcoders.hyperql.sample.jpa.bookstore_jpa.controller;
//
//
//
//
//public class BookStoreJpaControllerTest {
//
//    @Mock
//    private BookStoreJpaService service;
//
//    @Mock
//    private ConversionService conversionService;
//
//    @Mock
//    private HyperStorageController.CRUD crudController;
//
//    @InjectMocks
//    private BookStoreJpaController controller;
//
//    @BeforeEach
//    public void setUp() {
//        MockitoAnnotations.openMocks(this);
//        controller = new BookStoreJpaController(service, conversionService);
//    }
//
//    @Test
//    public void testNodes() throws Exception {
//        String table = "testTable";
//        OutputOptions req = new OutputOptions();
//        Map<String, Object> filter = Map.of("key", "value");
//
//        HyperStorageController.Response expectedResponse = new HyperStorageController.Response();
//        when(crudController.nodes(any(String.class), any(OutputOptions.class), any(Map.class))).thenReturn(expectedResponse);
//
//        HyperStorageController.Response actualResponse = controller.nodes(table, req, filter);
//
//        assertEquals(expectedResponse, actualResponse);
//    }
//}