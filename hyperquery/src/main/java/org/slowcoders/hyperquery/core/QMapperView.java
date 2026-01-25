package org.slowcoders.hyperquery.core;

import org.slowcoders.hyperquery.impl.HModel;
import org.slowcoders.hyperquery.impl.HSchema;
import org.slowcoders.hyperquery.impl.ViewResolver;

import java.util.Map;

public class QMapperView<R extends QRecord<?>> extends HModel {

    private final String namespace;
    private final String sqlId;
    private final Class<R> recordType;
    private HSchema schema;

    private final Map<String, String> properties;
    private Object query;

    public QMapperView(Class<R> recordType, Class<?> mapper, String sqlId) {
        this(recordType, mapper.getName(), sqlId);
    }

    public QMapperView(Class<R> recordType, String namespace, String sqlId) {
        this(recordType, namespace, sqlId, null);
    }

    public QMapperView(Class<R> recordType, Class<?> mapper, String sqlId, Map<String, String> properties) {
        this(recordType, mapper.getName(), sqlId, properties);
    }

    public QMapperView(Class<R> recordType, String namespace, String sqlId, Map<String, String> properties) {
        this.namespace = namespace;
        this.sqlId = sqlId;
        this.recordType = recordType;
        this.properties = properties;
    }

    @Override
    protected HSchema loadSchema() {
        if (schema == null) {
            schema = HSchema.loadSchema(recordType, false);
        }
        return schema;
    }

    @Override
    protected Object getTableExpression(ViewResolver viewResolver) {
        if (query == null) {
            query = viewResolver.resolveView(namespace, sqlId, properties);
        }
        return query;
    }

    @Override
    protected String getTableName() {
        return "";
    }

    public String getMapperId() {
        return namespace + '.' + sqlId;
    }

    public final Map<String, String> getProperties() {
        return properties;
    }
}
