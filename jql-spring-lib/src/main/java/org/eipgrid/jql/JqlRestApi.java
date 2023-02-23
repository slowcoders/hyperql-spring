package org.eipgrid.jql;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.eipgrid.jql.schema.QResultMapping;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1) select 가 지정되지 않으면, 쿼리 문에 사용된 Entity 들의 Property 들을 검색 결과에 포함시킨다.
 * 2) select 를 이용하여 검색 결과에 포함할 Property 들을 명시할 수 있다.
 *    - Joined Property 명만 명시된 경우, 해당 Joined Entity 의 모든 Property 를 선택.
 */
public interface JqlRestApi {

    @Getter
    class Response {

        @Schema(implementation = Object.class)
        private Map<String, Object> metadata;
        private Object content;

        @JsonIgnore
        private QResultMapping resultMapping;

        @Getter
        @JsonIgnore
        private JqlQuery query;

        private Response(Object content, QResultMapping resultMapping) {
            this.content = content;
            this.resultMapping = resultMapping;
        }

        public static Response of(Object content, JqlSelect select) {
            return new Response(content, select.resultMapping);
        }

        public void setProperty(String key, Object value) {
            if (this.metadata == null) {
                this.metadata = new HashMap<>();
            }
            this.metadata.put(key, value);
        }
    }


    default Response search(JqlEntitySet entitySet, String select, String[] orders, Integer page, Integer limit, Map<String, Object> filter) {
        JqlQuery query = entitySet.createQuery(filter);
        query.setSelection(select);
        if (orders != null) query.setSort(orders);
        boolean needPagination = false;
        if (limit != null) {
            query.setLimit(limit);
            if (page != null) {
                query.setOffset(page * limit);
            }
        }
        List<Object> result = query.getResultList();
        Response resp = Response.of(result, query.getSelection());
        resp.query = query;
        if (needPagination) {
            resp.setProperty("totalElements", query.count());
        }
        return resp;
    }

    static Sort.Order parseOrder(String column) {
        char first_ch = column.charAt(0);
        boolean ascend = first_ch != '-';
        String name = (ascend && first_ch != '+') ? column : column.substring(1);
        return ascend ? Sort.Order.asc(name) : Sort.Order.desc(name);
    }

    static Sort buildSort(String[] orders) {
        if (orders == null || orders.length == 0) {
            return Sort.unsorted();
        }
        ArrayList<Sort.Order> _orders = new ArrayList<>();
        for (String column : orders) {
            Sort.Order order = parseOrder(column);
            _orders.add(order);
        }
        return Sort.by(_orders);
    }

}