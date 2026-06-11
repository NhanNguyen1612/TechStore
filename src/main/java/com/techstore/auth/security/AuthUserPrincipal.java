package com.techstore.auth.security;

import com.techstore.auth.entity.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthUserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final long tokenVersion;
    private final List<GrantedAuthority> authorities;

    private AuthUserPrincipal(
            Long id,
            String email,
            String password,
            boolean enabled,
            long tokenVersion,
            List<GrantedAuthority> authorities
    ) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.tokenVersion = tokenVersion;
        this.authorities = authorities;
    }

    public static AuthUserPrincipal from(User user) {
        return new AuthUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.isEnabled() && !user.isDeleted(),
                user.getTokenVersion(),
                List.of(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }

    public Long getId() {
        return id;
    }

    public long getTokenVersion() {
        return tokenVersion;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
