package org.slowcoders.hyperquery.core;


import org.apache.ibatis.annotations.Select;

public interface QRelation {

    @Select("${__sql__}")
    Object __select__(Object params);
}
