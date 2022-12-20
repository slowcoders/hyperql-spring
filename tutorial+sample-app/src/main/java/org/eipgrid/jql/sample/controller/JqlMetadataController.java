package org.eipgrid.jql.sample.controller;

import org.eipgrid.jql.jdbc.JQLJdbcService;
import org.eipgrid.jql.jdbc.JdbcMetadataController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jql/metadata")
public class JqlMetadataController extends JdbcMetadataController {

    public JqlMetadataController(JQLJdbcService service) {
        super(service);
    }
}