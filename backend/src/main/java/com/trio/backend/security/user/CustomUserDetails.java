package com.trio.backend.security.user;

import com.trio.backend.entity.Permission;
import com.trio.backend.entity.Role;
import com.trio.backend.entity.User;
import com.trio.backend.entity.UserRole;
import com.trio.backend.enums.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID id;

    private final String email;

    private final String password;

    private final UserStatus status;

    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {

        this.id = user.getId();

        this.email = user.getEmail();

        this.password = user.getPassword();

        this.status = user.getStatus();

        this.authorities = buildAuthorities(user.getUserRoles());
    }

    /**
     * Constructs a CustomUserDetails with authorities from JWT claims,
     * avoiding a database query for roles and permissions.
     */
    public CustomUserDetails(User user, Collection<? extends GrantedAuthority> authorities) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.status = user.getStatus();
        this.authorities = authorities;
    }

    /**
     * Convertit les rôles et permissions de l'utilisateur
     * en GrantedAuthority utilisées par Spring Security.
     */
    private Collection<? extends GrantedAuthority> buildAuthorities(
            Set<UserRole> userRoles
    ) {

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        for (UserRole userRole : userRoles) {

            Role role = userRole.getRole();

            authorities.add(new SimpleGrantedAuthority(
                    "ROLE_" + role.getName().name()
            ));

            role.getRolePermissions().stream()
                    .map(rolePermission -> rolePermission.getPermission().getCode())
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        return authorities;
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
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.LOCKED && status != UserStatus.SUSPENDED && status != UserStatus.ARCHIVED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

}
