package org.slowcoders.jql.jdbc.timescale;

import org.slowcoders.jql.JqlColumn;
import org.slowcoders.jql.JqlSchema;
import org.slowcoders.jql.jdbc.JDBCRepositoryBase;
import org.slowcoders.jql.jdbc.JQLJdbcService;
import org.slowcoders.jql.jdbc.parser.SQLWriter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TimescaleRepository extends JDBCRepositoryBase {

    private final String timeKeyColumnName;
    private static final String SUFFIX_CONT_AGG = "_ts_agg";
    private static final String SUFFIX_HOURLY_VIEW = "_hourly";
    private static final String SUFFIX_DAILY_VIEW = "_daily";

    public TimescaleRepository(JQLJdbcService service, Class<?> entity) {
        super(service, entity);

        this.timeKeyColumnName = super.getSchema().getTimeKeyColumn().getColumnName();
        this.initializeTSDB();

    }

    public String getTimeKeyColumnName() {
        return timeKeyColumnName;
    }

    public void initializeTSDB() {
        JdbcTemplate jdbc = super.getJdbcTemplate();
        String sql = build_init_timescale(2);
        jdbc.execute(sql);
        try {
            sql = "SELECT 1 FROM " + super.getSchema().getTableName() + SUFFIX_DAILY_VIEW + " limit 1";
            List<Map<String, Object>> res = jdbc.queryForList(sql);
            if (res.size() > 0) return;
        }
        catch (Exception e) {
        }
        remove_down_sampling_view();
        // 6주를 기본 저장 간격으로 설정한다.
        sql = build_auto_down_sampling_view(7 * 6);
        jdbc.execute(sql);
    }

    private void execute_silently(String sql) {
        try {
            super.getJdbcTemplate().execute(sql);
        }
        catch (Exception e) {
            //e.printStackTrace();
        }
    }
    private void remove_down_sampling_view() {
        JqlSchema jqlSchema = super.getSchema();
        String tableName = jqlSchema.getTableName();
        String aggView = tableName + SUFFIX_CONT_AGG;
        execute_silently("SELECT remove_continuous_aggregate_policy('" + aggView + "');");
        execute_silently("DROP MATERIALIZED VIEW " + aggView + " cascade");
    }

    private void refresh_aggregation(Timestamp start, Timestamp end) {
        JqlSchema jqlSchema = super.getSchema();
        String tableName = jqlSchema.getTableName();
        String aggView = tableName + SUFFIX_CONT_AGG;

        String sql = "CALL refresh_continuous_aggregate('" + aggView + "', NULL, DATE_TRUNC('hour', now()));";
        super.getJdbcTemplate().execute(sql);
    }

    protected String build_init_timescale(int hours) {
        JqlSchema jqlSchema = super.getSchema();
        SQLWriter sb = new SQLWriter(jqlSchema);
        String ts_column = timeKeyColumnName;
        sb.writeF("SELECT create_hypertable('{0}', '{1}',", jqlSchema.getTableName(), ts_column)
                .write("if_not_exists => TRUE, migrate_data => true, ")
                .writeF("chunk_time_interval => interval '{0} hour')", Integer.toString(hours));
        return sb.toString();
    }

    protected String build_auto_down_sampling_view(int retention_days) {
        JqlSchema jqlSchema = super.getSchema();
        SQLWriter sb = new SQLWriter(jqlSchema);
        String tableName = jqlSchema.getTableName();
        String aggView = tableName + SUFFIX_CONT_AGG;
        JqlColumn ts_key = jqlSchema.getColumn(timeKeyColumnName);
        String ts_col_name = jqlSchema.getTimeKeyColumn().getColumnName();

        sb.write("CREATE MATERIALIZED VIEW IF NOT EXISTS ").writeln(aggView);
        sb.writeln("\tWITH (timescaledb.continuous)\nAS SELECT").incTab();
        sb.writeF("time_bucket('1 hour', {0}) AS time_h,\n", ts_col_name);

        for (JqlColumn col : jqlSchema.getPKColumns()) {
            if (col != ts_key) {
                sb.write(col.getColumnName()).writeln(",");
            }
        }

        ArrayList<JqlColumn> accColumns = new ArrayList<>();
        for (JqlColumn col : jqlSchema.getWritableColumns()) {
            String col_name = col.getColumnName();
            switch (col.getAggregationType()) {
                case Sum: {
                    String ss = "min({0}) as {0}_min,\n" +
                            "max({0}) as {0}_max,\n" +
                            "first({0}, {1}) as {0}_first,\n" +
                            "last({0}, {1}) as {0}_last,\n";
                    sb.writeF(ss, col_name, ts_col_name);
                    accColumns.add(col);
                    break;
                }
                case Mean: {
                    String ss = "avg({0}) as {0},\n";
                    sb.writeF(ss, col_name);
                    break;
                }
                case None:
                    break;
            }
        }

        sb.decTab().replaceTrailingComma("\nFROM ").writeTableName().writeln();
        sb.write("GROUP BY ");
        for (JqlColumn col : jqlSchema.getPKColumns()) {
            if (col == ts_key) {
                sb.write("time_h, ");
            }
            else {
                sb.write(col.getColumnName()).write(", ");
            }
        }
        sb.replaceTrailingComma(";\n\n");

        String retention_interval = retention_days <= 0 ? "NULL" : "INTERVAL '" + retention_days + " day'";
        sb.writeF("SELECT add_continuous_aggregate_policy('{0}',\n", aggView).incTab();
        sb.writeF("start_offset => {0},\n", retention_interval);
        sb.write("end_offset => INTERVAL '1 hour',\n" +
                 "schedule_interval => INTERVAL '1 hour');\n\n");
        sb.decTab();

        if (false && retention_days > 0) {
            // 설치 버전 문제인지 아래 함수 호출에 실패.
            sb.writeF("SELECT add_drop_chunks_policy('{0}', {1}, cascade_to_materializations=>FALSE);\n\n",
                    aggView, retention_interval);
        }

        sb.writeF("CREATE OR REPLACE VIEW {0} AS\nSELECT\n", tableName + SUFFIX_HOURLY_VIEW);
        if (accColumns.size() == 0) {
            sb.writeln("* FROM ").writeln(aggView).write(";\n\n");
        }
        else {
            sb.incTab();
            for (JqlColumn col : jqlSchema.getPKColumns()) {
                if (col == ts_key) {
                    sb.write("time_h, ");
                }
                else {
                    sb.write(col.getColumnName()).writeln(",");
                }
            }
            for (JqlColumn col : jqlSchema.getWritableColumns()) {
                switch (col.getAggregationType()) {
                    case Sum: {
                        String fmt =
                                "(case when {0}_last >= least({0}_first, lag({0}_last) over _w) then\n" +
                                "           {0}_max - least({0}_min, lag({0}_last) over _w)\n" +
                                "      else {0}_max - least({0}_first, lag({0}_last) over _w) + ({0}_last - {0}_min)\n" +
                                " end) as {0},\n";
                        sb.writeF(fmt, col.getColumnName());
                        break;
                    }
                    case Mean: {
                        sb.write(col.getColumnName()).writeln(",");
                        break;
                    }
                }
            }
            sb.decTab();
            sb.replaceTrailingComma("\nFROM ").writeln(aggView);
            sb.write("WINDOW _w AS(partition by ");
            for (JqlColumn col : jqlSchema.getPKColumns()) {
                if (col != ts_key) sb.write(col.getColumnName()).write(", ");
            }
            sb.replaceTrailingComma(" ORDER BY time_h);");
            sb.writeln();
        }

        sb.writeln();
        sb.writeF("CREATE OR REPLACE VIEW {0} AS\nSELECT\n", tableName + SUFFIX_DAILY_VIEW).incTab();
        sb.writeln("time_bucket('1 day', time_h) AS time_d,");
        for (JqlColumn col : jqlSchema.getPKColumns()) {
            if (col != ts_key) sb.write(col.getColumnName()).writeln(",");
        }
        for (JqlColumn col : jqlSchema.getWritableColumns()) {
            switch (col.getAggregationType()) {
                case Sum: {
                    sb.writeF("sum({0}) as {0},\n", col.getColumnName());
                    break;
                }
                case Mean: {
                    sb.writeF("avg({0}) as {0},\n", col.getColumnName());
                    break;
                }
            }
        }
        sb.decTab().replaceTrailingComma("\n");
        sb.writeF("FROM {0}\nGROUP BY ", tableName + SUFFIX_HOURLY_VIEW);
        for (JqlColumn col : jqlSchema.getPKColumns()) {
            if (col != ts_key) sb.write(col.getColumnName()).write(", ");
        }
        sb.writeln("time_d;");
        return sb.toString();
    }
}