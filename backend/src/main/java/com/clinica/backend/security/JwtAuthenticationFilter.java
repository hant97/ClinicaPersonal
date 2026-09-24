package com.clinica.backend.security;

import com.clinica.backend.model.User;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenRevocationService tokenRevocationService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)
                || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Un token inválido o expirado deja la solicitud sin autenticar: el entry point responde
        // 401 y el frontend renueva la sesión. Los fallos de infraestructura (por ejemplo, base
        // de datos caída) se propagan como 500 en lugar de convertirse en un 401 engañoso.
        Optional<Claims> claims = jwtService.parseClaims(authHeader.substring(BEARER_PREFIX.length()));
        if (claims.isPresent()) {
            authenticate(claims.get(), request);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(Claims claims, HttpServletRequest request) {
        String username = claims.getSubject();
        if (username == null) {
            return;
        }
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            log.debug("Token JWT de un usuario inexistente");
            return;
        }
        if (!(userDetails instanceof User user)
                || !user.isEnabled()
                || !username.equals(user.getUsername())
                || jwtService.tokenVersion(claims) != user.getTokenVersion()
                || tokenRevocationService.isRevoked(claims.getId())) {
            return;
        }
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
