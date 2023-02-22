package org.eipgrid.jql;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface JqlEntitySetController<ID> {

    JqlEntitySet<?, ID> getEntitySet();

    class Search<ID> implements JqlEntitySetController<ID> {
        private final JqlEntitySet<?, ID> entities;

        public Search(JqlEntitySet<?, ID> entities) {
            this.entities = entities;
        }

        public JqlEntitySet<?, ID> getEntitySet() {
            return entities;
        }

        @GetMapping(path = "/{id}")
        @Operation(summary = "지정 엔터티 읽기")
        @Transactional
        @ResponseBody
        public JqlQuery.Response get(@PathVariable("id") ID id,
                          @RequestParam(value = "select", required = false) String select$) {
            JqlSelect select = JqlSelect.of(select$);
            Object res = getEntitySet().find(id, select);
            if (res == null) {
                throw new HttpServerErrorException("Entity(" + id + ") is not found", HttpStatus.NOT_FOUND, null, null, null, null);
            }
            return JqlQuery.Response.of(res, select);
        }

        @PostMapping(path = "/find", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
        @Operation(summary = "엔터티 검색")
        @Transactional
        @ResponseBody
        public Object find_form(@Schema(example = "{ \"select\": \"\", \"sort\": \"\", \"page\": 0, \"limit\": 0, \"filter\": { } }")
                                @ModelAttribute JqlQuery.Request request) {
            return find(request);
        }

        @PostMapping(path = "/find", consumes = {MediaType.APPLICATION_JSON_VALUE})
        @Operation(summary = "엔터티 검색")
        @Transactional
        @ResponseBody
        public JqlQuery.Response find(@Schema(example = "{ \"select\": \"\", \"sort\": \"\", \"page\": 0, \"limit\": 0, \"filter\": { } }")
                                      @RequestBody JqlQuery.Request request) {
            return request.execute(getEntitySet());
        }

        @PostMapping(path = "/count")
        @Operation(summary = "엔터티 수 조회")
        @Transactional
        @ResponseBody
        public long count(@Schema(implementation = Object.class)
                          @RequestBody() HashMap<String, Object> jsFilter) {
            long count = getEntitySet().createQuery(jsFilter, null).count();
            return count;
        }
    }

    interface ListAll<ID> extends JqlEntitySetController<ID> {

        @GetMapping(path = "")
        @Operation(summary = "전체 목록")
        @Transactional
        @ResponseBody
        default JqlQuery.Response list(@RequestParam(value = "select", required = false) String select$,
                                       @RequestParam(value = "sort", required = false) String sort$) throws Exception {
            JqlSelect select = JqlSelect.of(select$);
            List<?> res = getEntitySet().findAll(select, JqlQuery.parseSort(sort$));
            return JqlQuery.Response.of(res, select);
        }
    }


    interface Insert<ID> extends JqlEntitySetController<ID> {

        @PutMapping(path = "/", consumes = {MediaType.APPLICATION_JSON_VALUE})
        @Operation(summary = "엔터티 추가")
        @Transactional
        @ResponseBody
        default <ENTITY> ENTITY add(@Schema(implementation = Object.class)
                       @RequestBody Map<String, Object> properties) throws Exception {
            JqlEntitySet<?, ID> table = getEntitySet();
            ENTITY entity = (ENTITY)table.insert(properties);
            return entity;
        }
    }

    interface Update<ID> extends JqlEntitySetController<ID> {

        @PatchMapping(path = "/{idList}", consumes = {MediaType.APPLICATION_JSON_VALUE})
        @Operation(summary = "엔터티 내용 변경")
        @Transactional
        @ResponseBody
        default JqlQuery.Response update(
                @Schema(type = "string", required = true) @PathVariable("idList") Collection<ID> idList,
                @RequestBody Map<String, Object> properties,
                @RequestParam(value = "select", required = false) String select$) throws Exception {
            JqlSelect select = JqlSelect.of(select$);
            JqlEntitySet<?, ID> table = getEntitySet();
            table.update(idList, properties);
            List<?> res = table.find(idList, select);
            return JqlQuery.Response.of(res, select);
        }
    }

    interface Delete<ID> extends JqlEntitySetController<ID> {
        @DeleteMapping("/{idList}")
        @ResponseBody
        @Operation(summary = "엔터티 삭제")
        @Transactional
        default Collection<ID> delete(@PathVariable("idList") Collection<ID> idList) {
            getEntitySet().delete(idList);
            return idList;
        }
    }


    /****************************************************************
     * Form Control API set
     */
    interface InsertForm<FORM, ID> extends JqlEntitySetController<ID> {

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
        public CRUD(JqlEntitySet<?, ID> repository) {
            super(repository);
        }
    }

}




