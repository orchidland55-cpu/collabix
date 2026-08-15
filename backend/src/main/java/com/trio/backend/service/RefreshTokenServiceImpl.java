package com.trio.backend.service;

import com.trio.backend.dto.auth.RefreshTokenResponse;
import com.trio.backend.entity.RefreshToken;
import com.trio.backend.entity.User;
import com.trio.backend.enums.UserStatus;
import com.trio.backend.exception.BadRequestException;
import com.trio.backend.repository.RefreshTokenRepository;
import com.trio.backend.repository.UserRepository;
import com.trio.backend.security.jwt.JwtProperties;
import com.trio.backend.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the service management des Refresh Tokens de Collabix.
 *
 * <p>Cette class centralise alle la logical mÃ©tier relatede aux tokens
 * de refreshedssement JWT, conformÃ©ment au workflow following :</p>
 *
 * <pre>
 * Connexion user
 *         â”‚
 *         â–¼
 * Backend generates :
 *   - Access Token (duration de vie shorte)
 *   - Refresh Token (duration de vie long)
 *         â”‚
 *         â–¼
 * Clinkt stocke les deux tokens
 *         â”‚
 *         â–¼
 * Access Token expiress
 *         â”‚
 *         â–¼
 * Clinkt sendinge le Refresh Token
 *         â”‚
 *         â–¼
 * Backend valid le Refresh Token
 *         â”‚
 *         â”œâ”€ Verifies l'expiration
 *         â”œâ”€ Verifies the status revoked
 *         â”œâ”€ Verifies the status de the user
 *         â”‚
 *         â–¼
 * RÃ©vocation de l'ancien Refresh Token (rotation)
 *         â”‚
 *         â–¼
 * Generation d'a new couple de tokens
 * </pre>
 *
 * <p><strong>Responsibilitys :</strong></p>
 * <ul>
 *     <li>Create a new Refresh Token pour un user.</li>
 *     <li>Validr un Refresh Token (expiration, revocation, status user).</li>
 *     <li>GÃ©nÃ©rer un nouvel Access Token Ã  partir d'un Refresh Token valid
 *         (avec rotation du Refresh Token).</li>
 *     <li>RÃ©voquer un Refresh Token specific.</li>
 *     <li>RÃ©voquer all Refresh Tokens actives of a user.</li>
 * </ul>
 *
 * <p><strong>Collaborators :</strong></p>
 * <ul>
 *     <li>{@link RefreshTokenRepository} for access aux givens.</li>
 *     <li>{@link UserRepository} for validation de the user.</li>
 *     <li>{@link JwtService} for generation of tokens JWT.</li>
 *     <li>{@link JwtProperties} pour les durations de validitÃ©.</li>
 * </ul>
 *
 * <p><strong>SÃ©curitÃ© :</strong></p>
 * <ul>
 *     <li>Rotation systÃ©matique du Refresh Token Ã  each refreshedssement.</li>
 *     <li>RÃ©vocation explicite aprÃ¨s utilisation (protection contre la reuse).</li>
 *     <li>Validation du status user avant alle generation de token.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final String TOKEN_TYPE = "Bearer";

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    /**
     * {@inheritDoc}
     */
    @Override
    public RefreshToken createRefreshToken(
            User user,
            String createdByIp,
            String createdByUserAgent,
            String deviceInfo
    ) {

        String tokenValue = jwtService.generateRefreshToken(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration()))
                .revoked(false)
                .createdByIp(createdByIp)
                .createdByUserAgent(createdByUserAgent)
                .deviceInfo(deviceInfo)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);

        log.debug("Refresh token created for user: {}", user.getEmail());

        return saved;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(String token) {

        // Step 1 — Look up the token in the database
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token."));

        // Step 2 — Verify the token has not expired
        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Refresh token has expired. Please log in again.");
        }

        // Step 3 — Verify the token has not been revoked
        if (refreshToken.isRevoked()) {
            // REUSE DETECTION: If a revoked token is presented, it may indicate token theft.
            // Revoke all active tokens for this user as a security measure.
            User revokedUser = refreshToken.getUser();
            if (revokedUser != null) {
                log.warn("Refresh token reuse detected for user: {}. Revoking all active tokens.",
                        revokedUser.getEmail());
                revokeAllUserTokens(revokedUser.getId());
            }
            throw new BadRequestException("Refresh token has been revoked.");
        }

        // Step 4 â€” Verify the associated user still exists
        User user = refreshToken.getUser();
        if (user == null) {
            throw new BadRequestException("Refresh token is not associated with any user.");
        }

        // Step 5 â€” Verify the user is enabled and active
        if (!user.isEnabled() || user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("User account is not active.");
        }

        return refreshToken;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Cette method implements la rotation Complete of tokens :</p>
     * <ol>
     *     <li>Valid le Refresh Token existant.</li>
     *     <li>RÃ©voque l'ancien Refresh Token (revoked = true, revokedAt = now).</li>
     *     <li>Generates un nouvel Access Token.</li>
     *     <li>Generates a new Refresh Token et le persiste.</li>
     *     <li>Met Ã  jour lastUsedAt sur l'ancien token revoked.</li>
     * </ol>
     */
    @Override
    public RefreshTokenResponse refreshAccessToken(String refreshToken) {
        try {
            return doRefreshAccessToken(refreshToken);
        } catch (OptimisticLockingFailureException ex) {
            log.warn("Refresh token rotation conflict (concurrent use of the same token): {}", ex.getMessage());
            throw new BadRequestException("Refresh token has been used already. Please sign in again.");
        }
    }

    private RefreshTokenResponse doRefreshAccessToken(String refreshToken) {

        RefreshToken oldToken = refreshTokenRepository.findByTokenWithLock(refreshToken)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token."));

        if (oldToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Refresh token has expired. Please log in again.");
        }

        if (oldToken.isRevoked()) {
            User revokedUser = oldToken.getUser();
            if (revokedUser != null) {
                log.warn("Refresh token reuse detected for user: {}. Revoking all active tokens.",
                        revokedUser.getEmail());
                revokeAllUserTokens(revokedUser.getId());
            }
            throw new BadRequestException("Refresh token has been revoked.");
        }

        User user = oldToken.getUser();
        if (user == null) {
            throw new BadRequestException("Refresh token is not associated with any user.");
        }

        if (!user.isEnabled() || user.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("User account is not active.");
        }

        oldToken.setLastUsedAt(Instant.now());
        oldToken.setRevoked(true);
        oldToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(oldToken);

        log.debug("Old refresh token revoked for user: {}", user.getEmail());

        // Step 4 â€” Generate a new access token
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshTokenValue = jwtService.generateRefreshToken(user);

        RefreshToken newToken = RefreshToken.builder()
                .token(newRefreshTokenValue)
                .user(user)
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiration()))
                .revoked(false)
                .createdByIp(oldToken.getCreatedByIp())
                .createdByUserAgent(oldToken.getCreatedByUserAgent())
                .deviceInfo(oldToken.getDeviceInfo())
                .build();

        refreshTokenRepository.save(newToken);

        log.info("Refresh token rotated successfully for user: {}", user.getEmail());

        // Step 6 â€” Build and return the response
        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenValue)
                .tokenType(TOKEN_TYPE)
                .expiresIn(jwtProperties.getAccessTokenExpiration())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void revokeRefreshToken(String token) {

        Optional<RefreshToken> optionalToken = refreshTokenRepository.findByTokenWithLock(token);

        if (optionalToken.isEmpty()) {
            log.warn("Attempted to revoke a non-existent refresh token.");
            return;
        }

        RefreshToken refreshToken = optionalToken.get();

        if (refreshToken.isRevoked()) {
            log.debug("Refresh token was already revoked.");
            return;
        }

        refreshToken.setRevoked(true);
        refreshToken.setRevokedAt(Instant.now());

        refreshTokenRepository.save(refreshToken);

        log.debug("Refresh token revoked for user: {}", refreshToken.getUser().getEmail());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void revokeAllUserTokens(UUID userId) {

        int revokedCount = refreshTokenRepository.revokeAllByUser(userId, Instant.now());

        if (revokedCount > 0) {
            log.info("Revoked {} active refresh tokens for user: {}", revokedCount, userId);
        } else {
            log.debug("No active refresh tokens to revoke for user: {}", userId);
        }
    }

}

