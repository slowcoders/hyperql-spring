package org.slowcoders.hyperquery.core;

//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntitySetController<ENTITY extends QEntity<ENTITY>>  {

    protected final HyperService<ENTITY> service;
    
    EntitySetController(HyperService<ENTITY> hyperService) {
        this.service = hyperService;
    }

        @GetMapping(path = "/{id}")
        @Transactional
        @ResponseBody
        public ENTITY get(
                @PathVariable("id") Object id) {
            ENTITY entity = service.getEntity(id);
            if (entity == null) {
                throw new HttpServerErrorException("Entity(" + id + ") is not found", HttpStatus.NOT_FOUND, null, null, null, null);
            }
            return entity;
        }


        @GetMapping(path = "")
//        @Operation(summary = "엔터티 검색")
        @Transactional
        @ResponseBody
        public List<ENTITY> list(QFilter<ENTITY> filter) {
            return service.findEntities(filter);
        }



        @PutMapping(path = "", consumes = {MediaType.APPLICATION_JSON_VALUE})
//        @Operation(summary = "엔터티 추가")
        @Transactional
        @ResponseBody
        public ENTITY add(
                @RequestBody ENTITY entity) {
            return service.insert(entity);
        }

        @PutMapping(path = "/add-all", consumes = {MediaType.APPLICATION_JSON_VALUE})
//        @Operation(summary = "엔터티 추가")
        @Transactional
        @ResponseBody
        public int addAll(
                @RequestBody List<ENTITY> entities) {
            return service.insert(entities);
        }


        @PatchMapping(path = "/{idList}", consumes = {MediaType.APPLICATION_JSON_VALUE})
//        @Operation(summary = "엔터티 내용 변경")
        @Transactional
        @ResponseBody
        public int update(@RequestBody ENTITY entity) throws Exception {
            return service.insertOrUpdate(entity);
        }

        @DeleteMapping("/{id}")
        @ResponseBody
//        @Operation(summary = "엔터티 삭제")
        @Transactional
        public Collection<Object> delete(@PathVariable("id") Collection<Object> idList) throws Exception {
            service.delete(idList);
            return idList;
        }


//        @PostMapping(path = "/", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
//        @ResponseBody
////        @Operation(summary = "엔터티 추가")
//        @Transactional
//        public <ENTITY> ENTITY add_form(@ModelAttribute FORM formData) throws Exception {
//            Map<String, Object> dataSet = convertFormDataToMap(formData);
//            return (ENTITY) getEntitySet().insert(dataSet);
//        }

}




