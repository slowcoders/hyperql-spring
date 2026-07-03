package org.slowcoders.basecamp.security.service;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenDTO {
    private String accessToken;
    private String refreshToken;
}
