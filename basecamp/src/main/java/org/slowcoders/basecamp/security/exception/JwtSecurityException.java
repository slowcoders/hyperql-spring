package org.slowcoders.basecamp.security.exception;

public class JwtSecurityException extends RuntimeException {

    public JwtSecurityException(String msg) {
        super(msg);
    }
}
