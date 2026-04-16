package ru.rt.rostelecom_tms.domain.users.exceptions;

public class UserRoleNotAllowedException extends RuntimeException {
    public UserRoleNotAllowedException(String message) {
        super(message);
    }
}
