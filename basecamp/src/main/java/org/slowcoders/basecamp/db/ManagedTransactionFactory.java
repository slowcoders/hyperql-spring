package org.slowcoders.basecamp.db;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;

import javax.sql.DataSource;
import java.sql.Connection;

public class ManagedTransactionFactory extends SpringManagedTransactionFactory {

    @Override
    public Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit) {
        return new ManangedTransaction(dataSource);
    }

    @Override
    public Transaction newTransaction(Connection conn) {
        return super.newTransaction(conn);
    }
}
