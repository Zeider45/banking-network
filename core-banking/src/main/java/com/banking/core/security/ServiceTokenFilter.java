package com.banking.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de autenticación servicio-a-servicio (API key / token de servicio).
 *
 * El Core Banking es un servicio interno: idealmente solo el Home Banking
 * (a través del gateway) debe poder invocar sus endpoints de negocio /api/**.
 * Como el aislamiento de red por sí solo no identifica al llamante, exigimos
 * además una cabecera {@code X-Service-Token} con un valor compartido. Así el
 * Home Banking se "identifica" ante el Core y cualquier otra llamada se rechaza.
 *
 * Los endpoints de Actuator (health, métricas de Prometheus) quedan exentos
 * para que el monitoreo y los healthchecks de Docker sigan funcionando.
 *
 * Si no se configura ningún token ({@code banking.service-token} vacío) el
 * filtro no exige nada: útil para ejecutar el servicio aislado en desarrollo.
 */
@Slf4j
public class ServiceTokenFilter extends OncePerRequestFilter {

    /** Cabecera HTTP donde viaja el token de servicio. */
    public static final String HEADER = "X-Service-Token";

    private final String expectedToken;

    public ServiceTokenFilter(String expectedToken) {
        this.expectedToken = expectedToken;
    }

    /** Actuator y la página de error quedan abiertos (Prometheus / healthchecks). */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Sin token configurado => no se exige (modo desarrollo sin seguridad).
        if (!StringUtils.hasText(expectedToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader(HEADER);
        if (!expectedToken.equals(provided)) {
            log.warn("Llamada rechazada a '{}': token de servicio ausente o inválido", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Token de servicio inválido o ausente\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
