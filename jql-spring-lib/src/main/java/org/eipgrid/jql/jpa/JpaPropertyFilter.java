package org.eipgrid.jql.jpa;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import org.eipgrid.jql.JqlLeafProperty;
import org.eipgrid.jql.JqlRestApi;
import org.eipgrid.jql.js.JsType;

import javax.persistence.Id;
import java.util.Map;

public class JpaPropertyFilter extends BeanPropertyWriter {
    private static final String JQL_RESULT_MAPPING_KEY = JqlRestApi.Response.JpaFilter.JQL_RESULT_MAPPING_KEY;
    private static final String JQL_INCLUDE_ID = "jql-include-id";

    private final BeanPropertyWriter writer;
    private final boolean isId;
    private final boolean isLeaf;

    public JpaPropertyFilter(BeanPropertyWriter writer) {
        super(writer);
        this.writer = writer;
        this.isId = writer.getAnnotation(Id.class) != null;
        this.isLeaf = writer.getAnnotation(JqlLeafProperty.class) != null || 
                JsType.of(writer.getType().getRawClass()).isPrimitive();
    }

    @Override
    public void serializeAsElement(Object bean, JsonGenerator gen,
                                   SerializerProvider prov) throws Exception {
        super.serializeAsElement(bean, gen, prov);
    }


    @Override
    public void serializeAsField(Object bean,
                                 JsonGenerator gen,
                                 SerializerProvider prov) throws Exception {

        Map<String, Object> mapping = (Map<String, Object>) prov.getAttribute(JQL_RESULT_MAPPING_KEY);
        Boolean include_id = (Boolean) prov.getAttribute(JQL_INCLUDE_ID);
        if (include_id != Boolean.FALSE && this.isId) {
            // do-nothing
        }
        else if (mapping != null) {
            String pname = this.getName();
            Object column = mapping.get(pname);
            if (column != null) {
                if (!isLeaf) {
                    prov.setAttribute(JQL_RESULT_MAPPING_KEY, column);
                    Boolean is_array = (this.getType().getContentType() != null);
                    if (is_array != include_id) {
                        prov.setAttribute(JQL_INCLUDE_ID, is_array);
                    }
                    writer.serializeAsField(bean, gen, prov);
                    if (is_array != include_id) {
                        prov.setAttribute(JQL_INCLUDE_ID, include_id);
                    }
                    prov.setAttribute(JQL_RESULT_MAPPING_KEY, mapping);
                    return;
                }
            }
            else if (!isLeaf || (!mapping.isEmpty() && mapping.get("*") == null)) {
                return;
            }
        }
        writer.serializeAsField(bean, gen, prov);
    }
}