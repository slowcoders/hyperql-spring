package org.slowcoders.basecamp.security.service;

public interface AuthService {
    TokenDTO login(String id, String password);

    void logout(String token);
}
