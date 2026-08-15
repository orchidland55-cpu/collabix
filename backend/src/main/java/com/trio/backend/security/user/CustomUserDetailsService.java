package com.trio.backend.security.user;

import com.trio.backend.entity.User;
import com.trio.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Charge un utilisateur par son email.
     * Cette méthode est appelée automatiquement par Spring Security
     * lors de l'authentification.
     */
    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmailWithRolesAndPermissions(email)

                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found with email: " + email
                        ));

        return new CustomUserDetails(user);
    }

}