package org.slowcoders.basecamp.db;

import lombok.Getter;

import java.time.ZoneId;
import java.util.TimeZone;

@Getter
public class TransactionInfo {
    private final String userId;
    private final ZoneId timeZoneId;
    private final String apiPath;

    public TransactionInfo(String userId, String useTimeZoneCd, String apiPath) {
        this.userId = userId;
        this.timeZoneId = TimeZone.getTimeZone(useTimeZoneCd).toZoneId();
        this.apiPath = apiPath;
    }
}
