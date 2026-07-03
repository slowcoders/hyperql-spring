package org.slowcoders.basecamp.app;

public class ServiceException extends RuntimeException {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(Exception e, String message) {
        super(message);
    }
}
