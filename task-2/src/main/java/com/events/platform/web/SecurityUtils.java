package com.events.platform.web;

import com.events.platform.domain.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static User currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null
                || !a.isAuthenticated()
                || a.getPrincipal() == null
                || "anonymousUser".equals(a.getPrincipal())) {
            return null;
        }
        if (a.getPrincipal() instanceof User u) {
            return u;
        }
        return null;
    }

    public static User requireUser() {
        User u = currentUser();
        if (u == null) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }
        return u;
    }
}
