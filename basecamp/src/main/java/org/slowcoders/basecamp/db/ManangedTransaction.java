package org.slowcoders.basecamp.db;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.transaction.SpringManagedTransaction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
public class ManangedTransaction extends SpringManagedTransaction {
    private static final boolean DEBUG = false;
    private String prevTimeZone;
    private String prevUserId;
    private String prevApiPath;
    private Connection conn;

    public ManangedTransaction(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (this.conn == null) {
            this.conn = super.getConnection();
            saveCurrentState();
            applyTimeZone(conn);
            dumpCurrentSettings();
        }
        return conn;
    }

    @Override
    public void close() throws SQLException {
        try {
            if (this.conn != null && prevTimeZone != null) {
                this.setSessionState(prevTimeZone, prevUserId, prevApiPath);
                dumpCurrentSettings();
            }
            this.conn = null;
        } finally {
            super.close();
        }
    }

    private void saveCurrentState() throws SQLException {
        ResultSet rs =null;
        try (Statement ps = conn.createStatement()) {
            rs = ps.executeQuery("SHOW TIME ZONE;");
            if (rs.next()) {
                this.prevTimeZone = rs.getString(1);
            }
            rs.close();

            String sql = "SELECT " +
                    "current_setting('hpms_session.user_id', true) as user_id, " +
                    "current_setting('hpms_session.api_path', true) as api_path";
            rs = ps.executeQuery(sql);
            if (rs.next()) {
                this.prevUserId = rs.getString("user_id");
                this.prevApiPath = rs.getString("api_path");
            }
            rs.close();
        }
        finally {
            if(rs!=null){
                try{
                    rs.close();
                } catch (SQLException e) {
                    log.error(e.getMessage());
                }

            }
        }
    }

    private String notNullString(Object v) {
        return v == null ? "" : v.toString();
    }

    private void setSessionState(String timeZone, String userId, String apiPath) throws SQLException {
        try (Statement ps = conn.createStatement()) {
            String sql = "SET TIME ZONE '" + timeZone + "';\n";
            sql +=  "SET hpms_session.user_id = '" + notNullString(userId) + "';";
            sql +=  "SET hpms_session.api_path = '" + notNullString(apiPath) + "';";
            ps.execute(sql);
        }
    }

    private void applyTimeZone(Connection conn) throws SQLException {
        try {
            TransactionInfo se = TransactionLogger.getCurrentSessionInfo();
            if (se != null) {
                String userId = notNullString(se.getUserId());
                String useTimeZoneCd = se.getTimeZoneId().getId();
                String apiPath = se.getApiPath();
                setSessionState(useTimeZoneCd, userId, apiPath);
            } else {
                this.prevTimeZone = null;
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
            this.prevTimeZone = null;
        }
        catch (Exception e) {
            this.prevTimeZone = null;
        }

    }

    private void dumpCurrentSettings() throws SQLException {
        if (!DEBUG) return;
        ResultSet rs =null;
        try (Statement ps = conn.createStatement()) {
            rs = ps.executeQuery("SHOW TIME ZONE;");
            if (rs.next()) {
                System.out.println("current-session timezone: " + rs.getString(1));
            }
            rs.close();

            String sql = "SELECT current_setting('hpms_session.user_id', true) as user_id, " +
                    "current_setting('hpms_session.api_path', true) as api_path";
            rs = ps.executeQuery(sql);
            if (rs.next()) {
                System.out.println("current-session user: " + rs.getString("user_id"));
                System.out.println("current-session chain: " + rs.getString("chain_id"));
                System.out.println("current-session property: " + rs.getString("property_id"));
                System.out.println("current-session api_path: " + rs.getString("api_path"));
            }
            rs.close();
        }   finally {
            if(rs!=null){
                try{
                    rs.close();
                } catch (SQLException e) {
                    log.error(e.getMessage());
                }

            }
        }
    }
}
