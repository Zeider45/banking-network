package com.banking.core.config;

import com.banking.core.security.ServiceTokenFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad HTTP del Core Banking.
 *
 * El Core Banking es un servicio interno. Aunque la autenticación del usuario
 * final vive en el Home Banking, exigimos un token de servicio (API key) en
 * las llamadas entre servicios para que el Home Banking se identifique ante el
 * Core. De esa forma el Core no confía ciegamente en cualquier cliente que
 * alcance su red.
 *
 * Actuator (health, métricas Prometheus) queda abierto; los endpoints /api/**
 * los protege el {@link ServiceTokenFilter}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Token compartido que el Home Banking debe presentar. Vacío => no se exige. */
    @Value("${banking.service-token:}")
    private String serviceToken;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Prometheus y health checks son accesibles sin auth
                .requestMatchers("/actuator/**").permitAll()
                // El resto se admite a nivel de Spring Security; la identidad
                // entre servicios la valida el ServiceTokenFilter de abajo.
                .anyRequest().permitAll()
            )
            // Exige X-Service-Token en /api/** (actuator queda exento en el filtro)
            .addFilterBefore(new ServiceTokenFilter(serviceToken),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** BCrypt para hashear contraseñas de usuarios */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
