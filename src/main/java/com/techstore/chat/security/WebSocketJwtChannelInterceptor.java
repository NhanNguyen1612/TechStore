package com.techstore.chat.security;

import com.techstore.auth.exception.AppException;
import com.techstore.auth.security.AuthUserDetailsService;
import com.techstore.auth.security.AuthUserPrincipal;
import com.techstore.auth.security.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class WebSocketJwtChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AuthUserDetailsService userDetailsService;

    public WebSocketJwtChannelInterceptor(
            JwtService jwtService,
            AuthUserDetailsService userDetailsService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message,
                StompHeaderAccessor.class
        );
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        try {
            String authorization = authorizationHeader(accessor);
            if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
                throw new BadCredentialsException("Missing WebSocket bearer token");
            }

            JwtService.ParsedToken token = jwtService.parseAccessToken(
                    authorization.substring(BEARER_PREFIX.length())
            );
            AuthUserPrincipal principal = (AuthUserPrincipal) userDetailsService
                    .loadUserByUsername(token.subject());
            boolean tokenMatchesUser = principal.getId().equals(token.userId())
                    && principal.getTokenVersion() == token.tokenVersion()
                    && principal.isEnabled();
            if (!tokenMatchesUser) {
                throw new BadCredentialsException("Invalid WebSocket bearer token");
            }

            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.getAuthorities()
            ));
            return message;
        } catch (AppException | BadCredentialsException exception) {
            throw new BadCredentialsException(
                    "Invalid WebSocket bearer token",
                    exception
            );
        } catch (RuntimeException exception) {
            throw new BadCredentialsException(
                    "Unable to authenticate WebSocket connection",
                    exception
            );
        }
    }

    private String authorizationHeader(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(
                HttpHeaders.AUTHORIZATION
        );
        if (authorization == null) {
            authorization = accessor.getFirstNativeHeader("authorization");
        }
        return authorization;
    }
}
