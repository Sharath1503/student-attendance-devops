package com.attendance.security;

import java.util.Optional;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Access to the authenticated principal from service code. */
@Service
public class CurrentUserService {

    public Optional<AppUserDetails> find() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserDetails user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public AppUserDetails require() {
        return find().orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Not authenticated"));
    }
}
