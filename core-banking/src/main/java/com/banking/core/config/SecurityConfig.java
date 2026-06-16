package com.banking.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de seguridad HTTP del Core Banking.
 *
 * El Core Banking es un servicio interno: no necesita autenticación JWT propia.
 * La seguridad real está en el Home Banking (que sí expone la UI al usuario).
 * Aquí solo protegemos actuator y habilitamos el encoder de contraseñas.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Prometheus y health checks son accesibles sin auth
                .requestMatchers("/actuator/**").permitAll()
                // Todos los endpoints del API están abiertos (red interna Docker)
                .anyRequest().permitAll()
            );

        return http.build();
    }

    /** BCrypt para hashear contraseñas de usuarios */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
