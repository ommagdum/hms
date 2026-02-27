package com.vinayakit.hms.security;

import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {
    public static UserDetailsImpl getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetailsImpl) {
            return (UserDetailsImpl) principal;
        }
        return null;
    }
}
