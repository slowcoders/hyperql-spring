package org.slowcoders.hyperquery.util;

public class SqlWriter extends SourceWriter<SqlWriter> {
    public SqlWriter() {
        super('\'', "''");
    }
}
