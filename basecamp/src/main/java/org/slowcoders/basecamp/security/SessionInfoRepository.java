package org.slowcoders.basecamp.security;

import org.springframework.security.core.AuthenticationException;

public interface SessionInfoRepository {
    SessionInfo loadSessionInfoByLoginId(String loginId) throws AuthenticationException;
}
