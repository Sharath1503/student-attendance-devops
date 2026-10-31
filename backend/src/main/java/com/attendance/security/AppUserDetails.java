package com.attendance.security;

import com.attendance.domain.Role;
import com.attendance.entity.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Security principal: a read-only snapshot of the logged-in user. */
public class AppUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final boolean enabled;

    public AppUserDetails(Long id, String username, String passwordHash, Role role, boolean enabled) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
    }

    public static AppUserDetails from(User user) {
        return new AppUserDetails(user.getId(), user.getUsername(), user.getPasswordHash(), user.getRole(), user.isEnabled());
    }

    public Long getId() { return id; }
    public Role getRole() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isEnabled() { return enabled; }
}
