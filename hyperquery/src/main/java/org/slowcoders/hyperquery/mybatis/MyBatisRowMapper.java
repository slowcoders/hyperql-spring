package org.slowcoders.hyperquery.mybatis;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.*;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class MyBatisRowMapper implements org.springframework.jdbc.core.RowMapper<Object> {


    private final Configuration configuration;
    private final PropertyMapping[] mappings;
    private final int mappedColumnCount;
    private final Class<?> resultType;


    public MyBatisRowMapper(Configuration configuration, ResultSetMetaData metaData, Class<?> resultType) throws SQLException {
        this.configuration = configuration;
        this.resultType = resultType;


        MetaClass clazz = MetaClass.forClass(resultType, configuration.getReflectorFactory());
        PropertyMapping[] mappings = new PropertyMapping[metaData.getColumnCount()];
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        int mappedColumnCount = 0;
        for (int idxColumn = 0; ++idxColumn <= mappings.length;) {
            String columnName = metaData.getColumnName(idxColumn);
            // 칼럼명이 "fieldName.subFieldName" 형태인 경우, subEntity 도 맵핑 가능하다.
            String propertyName = clazz.findProperty(columnName, configuration.isMapUnderscoreToCamelCase());
            if (propertyName == null) continue;
            // @see DefaultResultSetHandler.createAutomaticMappings, ResultSetWrapper.getTypeHandler
            Class<?> propertyType = clazz.getSetterType(propertyName);
            JdbcType jdbcType = JdbcType.forCode(metaData.getColumnType(idxColumn));
            TypeHandler<?> handler = null;
            if (typeHandlerRegistry.hasTypeHandler(propertyType, jdbcType)) {
                handler = typeHandlerRegistry.getTypeHandler(propertyType, jdbcType);
                if (handler == null || handler instanceof UnknownTypeHandler) {
                    final Class<?> javaType = resolveClass(metaData.getColumnClassName(idxColumn));
                    if (javaType != null && jdbcType != null) {
                        handler = typeHandlerRegistry.getTypeHandler(javaType, jdbcType);
                    } else if (javaType != null) {
                        handler = typeHandlerRegistry.getTypeHandler(javaType);
                    } else if (jdbcType != null) {
                        handler = typeHandlerRegistry.getTypeHandler(jdbcType);
                    }
                }
            }
            PropertyMapping mapping = new PropertyMapping(columnName, propertyName, idxColumn, handler);
            mappings[mappedColumnCount ++] = mapping;
        }


        this.mappings = mappings;
        this.mappedColumnCount = mappedColumnCount;
    }


    private Class<?> resolveClass(String className) {
        try {
            // #699 className could be null
            if (className != null) {
                return Resources.classForName(className);
            }
        } catch (ClassNotFoundException e) {
            // ignore
        }
        return null;
    }


    @Override
    public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
        Object result = configuration.getObjectFactory().create(resultType);
        MetaObject objectWrapper = configuration.newMetaObject(result);


        for (int i = 0; i < mappedColumnCount; i ++) {
            PropertyMapping mapping = mappings[i];
            Object value = mapping.typeHandler.getResult(rs, mapping.idxColumn);
            objectWrapper.setValue(mapping.ormProperty, value);
        }
        return result;
    }


    private static class PropertyMapping {
        final String dbColumn;
        final String ormProperty;
        final int idxColumn;
        final TypeHandler<?> typeHandler;
        private static final TypeHandler<Object> objectHandler = new ObjectTypeHandler();


        public PropertyMapping(String column, String property, int idxColumn, TypeHandler<?> handler) {
            if (handler == null || handler instanceof UnknownTypeHandler) {
                handler = objectHandler;
            }
            this.dbColumn = column;
            this.ormProperty = property;
            this.idxColumn = idxColumn;
            this.typeHandler = handler;
        }
    }
}
