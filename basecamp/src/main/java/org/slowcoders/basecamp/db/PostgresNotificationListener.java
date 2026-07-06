package org.slowcoders.basecamp.db;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public abstract class PostgresNotificationListener {
    private final String CHANNEL_NAME;

    private final DataSource dataSource;

    private ExecutorService executorService;
    private volatile boolean running = true;

    protected PostgresNotificationListener(String channelName, DataSource dataSource) {
        CHANNEL_NAME = channelName;
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void startListening() {
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(this::listenLoop);
        log.info("Postgres LISTEN loop initialized on channel: {}", CHANNEL_NAME);
    }

    private void listenLoop() {
        while (running) {
            log.info("Connecting for PG LISTEN/NOTIFY...");
            try (Connection conn = dataSource.getConnection()) {

                // 1. LISTEN 채널 구독 등록
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("LISTEN " + CHANNEL_NAME);
                }

                // 2. PostgreSQL 전용 인터페이스(PGConnection)로 캐스팅/unwrap
                PGConnection pgConn = conn.unwrap(PGConnection.class);
                log.info("Successfully subscribed to channel: {}", CHANNEL_NAME);
                while (running) {
                    // 5000ms 동안 통지가 올 때까지 대기 (timeout)
                    // pgjdbc 42.2.18 버전 이상부터 소켓 블록을 통해 CPU 자원 소모 없이 대기합니다.
                    PGNotification[] notifications = pgConn.getNotifications(5000);
                    if (notifications != null) {
                        for (PGNotification notification : notifications) {
                            log.info("Received notification - Channel: {}, Payload: {}",
                                    notification.getName(), notification.getParameter());

                            // Spring ApplicationEvent로 전파하여 수신부와 디커플링
                            notifyEvent(notification.getName(), notification.getParameter());
                        }
                    }
                }
            } catch (SQLException e) {
                log.error("Database connection lost in LISTEN loop. Retrying in 5 seconds...", e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } catch (Exception e) {
                log.error("Unexpected error in LISTEN loop", e);
                break;
            }
        }
    }

    protected abstract void notifyEvent(String channel, String parameter);

    @PreDestroy
    public void stopListening() {
        running = false;
        if (executorService != null) {
            executorService.shutdownNow();
        }
        log.info("Postgres LISTEN loop stopped.");
    }

    /* SQL
    --1. Trigger 생성
    CREATE OR REPLACE FUNCTION notify_user_changes()
    RETURNS trigger AS $$
    BEGIN
        IF TG_OP = 'DELETE' THEN
            PERFORM pg_notify('my_channel', OLD.user_id);
        ELSIF TG_OP = 'UPDATE' THEN
            PERFORM pg_notify('my_channel', NEW.user_id);
        END IF
        RETURN NULL;
     END;
     $$ LANGUAGE plpgsql;

     -- 2. Trigger 바인딩
     CREATE TRIGGER user_changed_trigger
     AFTER INSERT OR UPDATE ON bookstore.user
     FOR EACH ROW
     EXECUTE FUNCTION notify_user_changes();

     */
}
