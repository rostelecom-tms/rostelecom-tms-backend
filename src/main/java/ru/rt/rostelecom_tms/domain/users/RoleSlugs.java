package ru.rt.rostelecom_tms.domain.users;

import ru.rt.rostelecom_tms.domain.users.exceptions.UserRoleNotAllowedException;

public final class RoleSlugs {
    public static final String USER = "user";
    public static final String ADMIN = "admin";
    public static final String TEAMLEAD = "teamlead";

    private RoleSlugs() {
    }

    public static void assertNotReserved(String slug) {
        if (slug.equals(USER) || slug.equals(ADMIN) || slug.equals(TEAMLEAD)) {
            throw new UserRoleNotAllowedException("slug " + slug + " is reserved and cannot be set");
        }
    }
}
