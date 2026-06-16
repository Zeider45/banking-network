package com.banking.home.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro JWT: intercepta cada request HTTP, extrae el token del header
 * "Authorization: Bearer {token}" y autentica al usuario en el SecurityContext.
 *
 * Si el token es inválido o está ausente, la cadena de filtros continúa
 * sin autenticación (Spring Security rechazará el acceso si el endpoint lo requiere).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Si no hay header o no empieza con "Bearer ", continúa sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // Quita "Bearer "

        if (!jwtUtil.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extrae datos del token y construye el objeto de autenticación
        String email = jwtUtil.extractEmail(token);
        String bankCode = jwtUtil.extractBankCode(token);
        String accountNumber = jwtUtil.extractAccountNumber(token);

        // Crea un principal con los datos del usuario (banco + cuenta)
        BankingUserPrincipal principal = new BankingUserPrincipal(email, bankCode, accountNumber);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        // Registra la autenticación en el contexto de Spring Security
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Usuario autenticado via JWT: {}, banco: {}", email, bankCode);

        filterChain.doFilter(request, response);
    }
}
