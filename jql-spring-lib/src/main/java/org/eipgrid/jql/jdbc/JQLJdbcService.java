package org.eipgrid.jql.jdbc;

import org.eipgrid.jql.JqlSchema;
import org.eipgrid.jql.SchemaLoader;
import org.eipgrid.jql.jdbc.metadata.JdbcSchemaLoader;
import org.eipgrid.jql.spring.JQLRepository;
import org.eipgrid.jql.spring.JQLService;
import org.eipgrid.jql.util.AttributeNameConverter;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

@Service
public class JQLJdbcService extends JQLService {
    JdbcSchemaLoader jdbcSchemaLoader;
    private HashMap<String, JQLRepository> repositories = new HashMap<>();

    public JQLJdbcService(DataSource dataSource, TransactionTemplate transactionTemplate,
                          MappingJackson2HttpMessageConverter jsonConverter,
                          ConversionService conversionService,
                          RequestMappingHandlerMapping handlerMapping,
                          EntityManager entityManager,
                          EntityManagerFactory entityManagerFactory) throws Exception {
        super(dataSource, transactionTemplate, jsonConverter, conversionService,
                handlerMapping, entityManager, entityManagerFactory);
        jdbcSchemaLoader = new JdbcSchemaLoader(dataSource, AttributeNameConverter.defaultConverter);
    }

    public SchemaLoader getSchemaLoader() {
        return jdbcSchemaLoader;
    }

    public JQLRepository makeRepository(String tableName) {
        JQLRepository repo = repositories.get(tableName);
        if (repo == null) {
            JqlSchema jqlSchema = jdbcSchemaLoader.loadSchema(tableName);
            repo = new JDBCRepositoryBase(this, jqlSchema);
            repositories.put(tableName, repo);
        }
        return repo;
    }

    public JqlSchema loadSchema(String tablePath) {
        return jdbcSchemaLoader.loadSchema(tablePath);
    }

    public JqlSchema loadSchema(Class entityType) {
        return jdbcSchemaLoader.loadSchema(entityType);
    }

    public List<String> getTableNames(String dbSchema) throws SQLException {
        return jdbcSchemaLoader.getTableNames(dbSchema);
    }

    public List<String> getDBSchemas() {
        return jdbcSchemaLoader.getDBSchemas();
    }
}