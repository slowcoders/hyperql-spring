package org.slowcoders.basecamp.app.model;

import lombok.Builder;
import lombok.Data;
import org.slowcoders.basecamp.security.SessionInfo;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserDto implements SessionInfo {
    private String loginId;
    private String password;
    private String email;
    private String name;
    private OffsetDateTime createdAt;

    private Object profile;

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public ZoneId getUserTimeZone() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }
}



