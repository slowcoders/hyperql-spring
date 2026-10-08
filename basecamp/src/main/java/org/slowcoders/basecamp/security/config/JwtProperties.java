package org.slowcoders.basecamp.security.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JwtProperties {
    private String header;
    private String secret;
    private String refreshTokenSecret;
    private Long accessTokenValidityInSeconds;
    private Long refreshTokenValidityInSeconds;
}
