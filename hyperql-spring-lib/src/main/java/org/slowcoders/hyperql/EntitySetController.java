package org.slowcoders.hyperql;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.transaction.Transactional;
import org.slowcoders.hyperql.js.JsUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface EntitySetController<ID> extends RestTemplate {

    EntitySet<?, ID> getEntitySet();

    class Search<ID> implements EntitySetController<ID> {
        private final EntitySet<?, ID> entities;

        public Search(EntitySet<?, ID> entities) {
            this.entities = entities;
        }

        public EntitySet<?, ID> getEntitySet() {
            return entities;
        }

        @GetMapping(path = "/{id}")
        @Operation(summary = "지정 엔터티 읽기")
        @Transactional
        @ResponseBody
        public Response get(
                @Schema(implementation = String.class)
                @PathVariable("id") ID id,
                @RequestParam(value = "select", required = false) String select$) {
            HyperSelect select = HyperSelect.of(select$);
            Object res = getEntitySet().find(id, select);
            if (res == null) {
                throw new HttpServerErrorException("Entity(" + id + ") is not found", HttpStatus.NOT_FOUND, null, null, null, null);
            }
            return Response.of(res, select);
        }

        @PostMapping(path = "/find", consumes = {MediaType.APPLICATION_JSON_VALUE})
        @Operation(summary = "엔터티 검색")
        @Transactional
        @ResponseBody
        public Response find(
                @RequestParam(value = "output", required = false) OutputFormat output,
                @RequestParam(value = "select", required = false) String select,
                @Schema(implementation = String.class)
                @RequestParam(value = "sort", required = false) String[] orders,
                @RequestParam(value = "page", required = false) Integer page,
                @RequestParam(value = "limit", required = false) Integer limit,
                @RequestParam(value = "distinct", required = false) Boolean distinct,
                @Schema(implementation = Object.class)
                @RequestBody Map<String, Object> filter) {
            return search(getEntitySet(), output, select, orders, page, limit, filter, distinct);
        }

        @PostMapping(path = "/count")
        @Operation(summary = "엔터티 수 조회")
        @Transactional
        @ResponseBody
        public long count(
                @Schema(implementation = Object.class)
                @RequestBody HashMap<String, Object> jsFilter) {
            long count = getEntitySet().createQuery(jsFilter).count();
            return count;
        }

        @PostMapping("/schema")
        @ResponseBody
        @Operation(summary = "엔터티 속성 정보 요약")
        public String schema() {
            EntitySet<?, ID> entitySet = getEntitySet();
            String schema = "<<No Schema>>";
            if (entitySet instanceof HyperRepository) {
                HyperRepository repository = (HyperRepository) entitySet;
                schema = JsUtil.getSimpleSchema(repository.getSchema(), false);
            }
            return schema;
        }
    }

    interface ListAll<ID> extends EntitySetController<ID> {

        @GetMapping(path = "")
        @Operation(summary = "전체 목록")
        @Transactional
        @ResponseBody
        default Response list(
                @RequestParam(value = "output", required = false) OutputFormat output,
                @RequestParam(value = "select", required = false) String select,
                @Schema(implementation = String.class)
                @RequestParam(value = "sort", required = false) String[] orders,
                @RequestParam(value = "page", required = false) Integer page,
                @RequestParam(value = "limit", required = false) Integer limit,
                @RequestParam(value = "distinct", required = false) Boolean distinct) throws Exception {
            return search(getEntitySet(), output, select, orders, page, limit, null, distinct);
        }
    }


    interface Insert<ID> extends EntitySetController<ID> {

        @PutMapping(path = "/", consumes = {MediaType.APPLICATION_JSON_VALUE})
        @Operation(summary = "엔터티 추가")
        @Transactional
        @ResponseBody
        default <ENTITY> ENTITY add(
                @Schema(implementation = Object.class)
                @RequestBody Map<String, Object> properties) throws Exception {
            EntitySet<?, ID> table = getEntitySet();
            ENTITY entity = (ENTITY)table.insert(properties);
            return entity;
        }

        @PutMapping(path = "/add-all", consumes = {MediaType.APPLICATION_JSON_VALUE})
        @Operation(summary = "엔터티 추가")
        @Transactional
        @ResponseBody
        default <ENTITY> ENTITY addAll(
                @RequestParam(value = "onConflict", required = false) String onConflict,
                @Schema(implementation = Object.class)
                @RequestBody List<Map<String, Object>> entities) throws Exception {
            EntitySet.InsertPolicy insertPolicy = parseInsertPolicy(onConflict);
            EntitySet<?, ID> table = getEntitySet();
            ENTITY entity = (ENTITY)table.insert(entities, insertPolicy);
            return entity;
        }
    }

    interface Update<ID> extends EntitySetController<ID> {

        @PatchMapping(path = "/{idList}", consumes = {MediaType.APPLICATION_JSON_VALUE})
        @Operation(summary = "엔터티 내용 변경")
        @Transactional
        @ResponseBody
        default Response update(
                @Schema(type = "string", required = true)
                @PathVariable("idList") Collection<ID> idList,
                @RequestParam(value = "select", required = false) String select$,
                @Schema(implementation = Object.class)
                @RequestBody Map<String, Object> properties) throws Exception {
            HyperSelect select = HyperSelect.of(select$);
            EntitySet<?, ID> table = getEntitySet();
            table.update(idList, properties);
            List<?> res = table.find(idList, select);
            return Response.of(res, select);
        }
    }

    interface Delete<ID> extends EntitySetController<ID> {
        @DeleteMapping("/{id}")
        @ResponseBody
        @Operation(summary = "엔터티 삭제")
        @Transactional
        default Collection<ID> delete(@PathVariable("id") Collection<ID> idList) {
            getEntitySet().delete(idList);
            return idList;
        }
    }


    /****************************************************************
     * Form Control API set
     */
    interface InsertForm<FORM, ID> extends EntitySetController<ID> {

        @PostMapping(path = "/", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
        @ResponseBody
        @Operation(summary = "엔터티 추가")
        @Transactional
        default <ENTITY> ENTITY add_form(@ModelAttribute FORM formData) throws Exception {
            Map<String, Object> dataSet = convertFormDataToMap(formData);
            return (ENTITY) getEntitySet().insert(dataSet);
        }

        Map<String, Object> convertFormDataToMap(FORM formData);
    }

    class CRUD<ID> extends Search<ID> implements Insert<ID>, Update<ID>, Delete<ID> {
        public CRUD(EntitySet<?, ID> repository) {
            super(repository);
        }
    }

}




