package ru.rt.rostelecom_tms.domain.users.exceptions;

public class UserRoleNotFoundException extends RuntimeException {

	public UserRoleNotFoundException() {
		super("User role not found");
	}

	public UserRoleNotFoundException(String message) {
		super(message);
	}
}
