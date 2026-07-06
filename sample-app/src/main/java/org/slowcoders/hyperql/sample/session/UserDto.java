package org.slowcoders.hyperql.sample.session;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slowcoders.basecamp.security.SessionInfo;
import org.springframework.security.core.GrantedAuthority;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto implements SessionInfo {
    private String userId;
    private String password;
    private String email;
    private String name;
    private OffsetDateTime createdAt;

    private Object profile;

    @Hidden
    @Override
    public String getLoginId() {
        return userId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Hidden
    @Override
    public String getUsername() {
        return userId;
    }

    @Hidden
    @Override
    public ZoneId getUserTimeZone() {
        return ZoneId.systemDefault();
    }

    @Hidden
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }
}



