package org.slowcoders.hyperquery.core;


import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface QRepository<ENTITY extends QEntity> {

    @Select("${__sql__}")
    Object __select__(Object params);

    @Update("""
        WITH _DATA AS (
            ${__input_data__}
        ), _OLD AS (
            SELECT * FROM ${__table_name__} AS _OLD
            WHERE ${__filter__}
        ), _NEW AS (
            INSERT INTO ${__table_name__} (${__column_names__})
            SELECT * FROM _DATA
            ON CONFLICT (${__primary_keys__})
            DO UPDATE SET ${__assign_values__}
            RETURNING *
        )
    """)
    Object __insert_or_update__(ENTITY entity);

    @Update("""
        WITH _DATA AS (
            ${__input_data__}
        ), _OLD AS (
            SELECT * FROM ${__table_name__} AS OLD
            WHERE ${__filter__}
        ), _NEW AS (
            UPDATE ${__table_name__} NEW
            SET ${__assign_values__}
            FROM _OLD
            WHERE ${__new_pk_tuple__} IN (${__old_pk_tuple__})
            RETURNING *
        )
    """)
    <T extends ENTITY> QRecord<T> __update__(QRecord<T> record, QFilter<T> filter);
}
