package com.trio.backend.websocket;

import com.trio.backend.entity.Notification;
import com.trio.backend.enums.TokenType;
import com.trio.backend.security.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private static final String ATTR_USER_ID = "userId";
    private static final String PARAM_TOKEN = "token";

    private final Map<UUID, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final JwtService jwtService;

    public NotificationWebSocketHandler(ObjectMapper objectMapper, JwtService jwtService) {
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID userId = resolveAuthenticatedUserId(session);
        if (userId == null) {
            try {
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Invalid or missing access token"));
            } catch (IOException e) {
                log.warn("Failed to close unauthorized WebSocket session {}", session.getId(), e);
            }
            return;
        }
        session.getAttributes().put(ATTR_USER_ID, userId);
        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.debug("WebSocket connected: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object attr = session.getAttributes().get(ATTR_USER_ID);
        if (attr instanceof UUID userId) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
            log.debug("WebSocket disconnected: userId={}, sessionId={}", userId, session.getId());
        }
    }

    public void sendNotification(UUID recipientId, Notification notification) {
        Set<WebSocketSession> sessions = userSessions.get(recipientId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(notification);
            TextMessage message = new TextMessage(payload);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        log.warn("Failed to send WebSocket message to session={}", session.getId(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to serialize notification for WebSocket", e);
        }
    }

    /**
     * Authenticates the WebSocket by validating the access token supplied via
     * the {@code token} query parameter and deriving the user id from its signed
     * claims. The recipient is never taken from an untrusted {@code userId} param,
     * which prevents a user from subscribing to another user's notifications (IDOR).
     */
    private UUID resolveAuthenticatedUserId(WebSocketSession session) {
        String token = queryParam(session, PARAM_TOKEN);
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            if (jwtService.isTokenValid(token, TokenType.ACCESS)) {
                return jwtService.extractUserId(token);
            }
        } catch (IllegalArgumentException e) {
            log.warn("Invalid token for WebSocket session {}", session.getId());
        }
        return null;
    }

    private String queryParam(WebSocketSession session, String name) {
        String query = session.getUri().getQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
