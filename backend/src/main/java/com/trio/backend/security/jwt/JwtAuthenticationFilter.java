package com.trio.backend.security.jwt;

import com.trio.backend.entity.User;
import com.trio.backend.enums.TokenType;
import com.trio.backend.enums.UserStatus;
import com.trio.backend.repository.UserRepository;
import com.trio.backend.security.user.CustomUserDetails;
import com.trio.backend.security.user.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final CustomUserDetailsService userDetailsService;

    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(

            HttpServletRequest request,

            HttpServletResponse response,

            FilterChain filterChain

    ) throws ServletException, IOException {

        final String authorizationHeader =
                request.getHeader("Authorization");

        if (!StringUtils.hasText(authorizationHeader)
                || !authorizationHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);

            return;
        }

        String token = authorizationHeader.substring(7);

        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtService.isTokenValid(token, TokenType.ACCESS)) {

            filterChain.doFilter(request, response);

            return;
        }

        String email = jwtService.extractUsername(token);

        if (email != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = buildUserDetailsFromToken(token, email);

            if (userDetails == null) {
                filterChain.doFilter(request, response);
                return;
            }

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent() && userOpt.get().getStatus() != UserStatus.ACTIVE) {
                filterChain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(

                            userDetails,

                            null,

                            userDetails.getAuthorities()

                    );

            authentication.setDetails(

                    new WebAuthenticationDetailsSource()

                            .buildDetails(request)

            );

            SecurityContextHolder.getContext()

                    .setAuthentication(authentication);

        }

        filterChain.doFilter(request, response);

    }

    /**
     * Builds UserDetails from JWT claims when permissions are embedded in the token,
     * falling back to database query for tokens without permission claims.
     */
    private UserDetails buildUserDetailsFromToken(String token, String email) {
        List<String> permissionCodes = jwtService.extractPermissions(token);

        if (!permissionCodes.isEmpty()) {
            return userRepository.findByEmail(email)
                    .map(user -> buildWithJwtPermissions(user, token, permissionCodes))
                    .orElse(null);
        }

        try {
            return userDetailsService.loadUserByUsername(email);
        } catch (Exception e) {
            log.warn("Failed to load user from database: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Builds CustomUserDetails using permissions from JWT + roles from JWT,
     * avoiding the heavy 4-JOIN database query.
     */
    private CustomUserDetails buildWithJwtPermissions(User user, String token, List<String> permissionCodes) {
        Collection<? extends GrantedAuthority> authorities = buildAuthorities(token, permissionCodes);
        return new CustomUserDetails(user, authorities);
    }

    private Collection<? extends GrantedAuthority> buildAuthorities(String token, List<String> permissionCodes) {
        Stream<String> roleAuthorities = jwtService.extractRoles(token).stream()
                .map(role -> "ROLE_" + role);

        Stream<String> permissionAuthorities = permissionCodes.stream();

        return Stream.concat(roleAuthorities, permissionAuthorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

}