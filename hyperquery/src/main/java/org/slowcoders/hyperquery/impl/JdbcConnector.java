package org.slowcoders.hyperquery.impl;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;

public interface JdbcConnector {

    public <T> T execute(ConnectionCallback<T> action) throws DataAccessException;

}
