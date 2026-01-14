package org.slowcoders.hyperquery.core;


import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface QRepository {

    @Select("${__sql__}")
    Object __select__(@Param("_parameter") Object params, @Param("__sql__") Object __sql__);

    @Update("""
        WITH _DATA AS (
            ${__input_data__}
        ), _OLD AS (
            SELECT * FROM ${__table_name__} AS _OLD
            WHERE ${__filter__}
        ), _NEW AS (
            INSERT INTO ${__table_name__} (${__column_names__})
            SELECT * FROM _DATA
            RETURNING *
        )
    """)
    Object __insert__(QEntity<?> entity);

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
    Object __insert_or_update__(QEntity<?> entity);

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
    QRecord<?> __updateAll__(QRecord<?> record, QFilter<?> filter);

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
    QRecord<?> __update__(QUniqueRecord<?> record);

}
