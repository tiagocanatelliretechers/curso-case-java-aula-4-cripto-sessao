package com.thrive.portal.config;

import com.thrive.portal.security.JwtAuthFilter;
import com.thrive.portal.security.MigratingPasswordEncoder;
import com.thrive.portal.security.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Estado apos a AULA 3 (auth/authz).
 * Corrigido aqui: BCrypt (Lab 3.3), rate limiting (Lab 3.4), /admin exige ADMIN (Lab 3.5).
 * AINDA vulneravel (proximas aulas): CSRF desabilitado e session fixation (Aula 4),
 * JWT sem verificacao (Aula 4), Actuator/H2 abertos e headers ausentes (Aulas 5-6).
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, RateLimitFilter rateLimitFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new MigratingPasswordEncoder();   // Lab 3.3
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)   // ainda desabilitado (Aula 4)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/", "/login", "/registrar", "/esqueci-senha",
                        "/css/**", "/js/**", "/webjars/**",
                        "/h2-console/**", "/actuator/**",
                        "/api/auth/**", "/produtos", "/produtos/buscar")
                    .permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")   // Lab 3.5
                .requestMatchers("/api/**").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/pedidos", true)
                .permitAll()
            )
            .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fixation -> fixation.none())   // ainda vulneravel (Aula 4)
            )
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)   // Lab 3.4
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
