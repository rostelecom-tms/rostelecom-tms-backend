package ru.rt.rostelecom_tms.domain.users.exceptions;

public class UserNotCreatedException extends RuntimeException {
    public UserNotCreatedException(String message) {
        super(message);
    }
}
