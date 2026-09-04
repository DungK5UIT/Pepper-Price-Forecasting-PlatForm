package com.giatieuviet.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Who may reach what.
 *
 * The platform has no user accounts and does not need them yet — the public
 * API is read-only and the data on it is published prices. What did need
 * closing is that {@code /internal/**} was reachable by anyone who could
 * reach the process: it exists so the ML service can pull training data
 * (ADR-0003), and it hands back the entire price series in one request. CORS
 * kept browsers out; it does nothing about anything else.
 *
 * So this draws one line rather than building an auth system: the read API
 * stays open, the internal and management surfaces do not.
 */
@Configuration
public class SecurityConfig {

    /** The role held by the ML service, not by any person. */
    private static final String INTERNAL_ROLE = "INTERNAL";

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // No browser session and no cookies: every caller authenticates
                // per request, so there is nothing for a forged cross-site
                // request to ride on.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        // The public contract in docs/api/README.md. Read-only,
                        // and the prices on it are published by their sources.
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        // Liveness for an uptime check. Detail is gated
                        // separately by management.endpoint.health.show-details,
                        // so an anonymous caller sees the status and nothing more.
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/internal/**").hasRole(INTERNAL_ROLE)
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    /**
     * One credential, held by the ML service. Deliberately not a database
     * table: there is no user model here, and inventing one to hold a single
     * machine account would be the wrong shape.
     *
     * The password has no default. A blank or well-known fallback is how an
     * internal endpoint ends up effectively open, so a deployment that has
     * not set one fails to start instead.
     */
    @Bean
    public UserDetailsService internalUsers(
            PasswordEncoder passwordEncoder,
            @Value("${app.internal-api.username}") String username,
            @Value("${app.internal-api.password}") String password) {
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles(INTERNAL_ROLE)
                .build());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
