package org.slowcoders.basecamp.app.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.extern.slf4j.Slf4j;
import org.slowcoders.basecamp.db.TransactionLogger;
import org.slowcoders.basecamp.db.TransactionInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Configuration
public class DateTimeConfig {

    private static final String dateTimePattern = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter dateTimeFormater = DateTimeFormatter.ofPattern(dateTimePattern);

    @Bean
    public Module hpssModule() {
        SimpleModule hpssModule = new SimpleModule();
        hpssModule.addSerializer(OffsetDateTime.class, new OffsetDateTimeSerializer(this));
        return hpssModule;
    }

    protected ZoneId getCurrentSessionTimeZone() {
        try {
            TransactionInfo si = TransactionLogger.getCurrentSessionInfo();
            return si == null ? null : si.getTimeZoneId();
        } catch (NullPointerException e) {
            log.error(e.getMessage());
            return null;
        } catch (Exception ignore) {
            return null;
        }
    }

    static class OffsetDateTimeSerializer extends JsonSerializer<OffsetDateTime> {

        private final DateTimeConfig config;

        OffsetDateTimeSerializer(DateTimeConfig config) {
            this.config = config;
        }
        @Override
        public void serialize(OffsetDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            ZoneId tz = config.getCurrentSessionTimeZone();
            String text = tz == null ? value.toString() : value.atZoneSameInstant(tz).format(dateTimeFormater);
            gen.writeString(text);
        }
    }
}
