package org.slowcoders.basecamp.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "hpss.auth")
public class AuthProperties {
    private List<String> publicUrls = new ArrayList<>();
    private JwtProperties jwtConfig;
}
