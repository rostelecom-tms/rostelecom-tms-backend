package ru.rt.rostelecom_tms.domain.users.exceptions;

public class UserRoleNotCreatedException extends RuntimeException {
    public UserRoleNotCreatedException(String message) {
        super(message);
    }
}
