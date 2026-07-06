package org.slowcoders.basecamp.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "basecamp.auth")
public class AuthProperties {
    private String secret;
    private Long accessTokenPeriodInSec;
    private Long refreshTokenPeriodInSec;

    private List<String> publicUrls = new ArrayList<>();
}
