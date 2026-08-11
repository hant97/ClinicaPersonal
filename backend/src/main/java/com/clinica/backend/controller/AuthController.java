package com.clinica.backend.controller;

import com.clinica.backend.dto.AuthRequest;
import com.clinica.backend.dto.AuthResponse;
import com.clinica.backend.repository.UserRepository;
import com.clinica.backend.security.JwtService;
import com.clinica.backend.security.LoginAttemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        String username = request.getUsername();

        if (loginAttemptService.isBlocked(username)) {
            long remaining = loginAttemptService.getRemainingLockMinutes(username);
            throw new LockedException("Cuenta bloqueada temporalmente por múltiples intentos fallidos. Intente nuevamente en " + remaining + " minuto(s).");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            loginAttemptService.loginFailed(username);
            throw ex;
        }

        loginAttemptService.loginSucceeded(username);

        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Usuario o contraseña incorrectos"));
        var jwtToken = jwtService.generateToken(user);
        return ResponseEntity.ok(AuthResponse.builder().token(jwtToken).build());
    }
}
