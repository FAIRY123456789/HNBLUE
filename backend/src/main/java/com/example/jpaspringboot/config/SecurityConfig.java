package com.example.jpaspringboot.config;

import com.example.jpaspringboot.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public UserDetailsService noFormLoginUserDetailsService() {
        return username -> { throw new UsernameNotFoundException("form login is disabled"); };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Credential bootstrap and diagnostic cache operations are never public APIs.
                        .requestMatchers("/api/internal/**").hasRole("ADMIN")
                        .requestMatchers("/setRedisData", "/getRedisData").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/indicators/update").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // Existing PUBLIC accounts keep using the same JWT; only anonymous access is closed.
                        .requestMatchers("/user/**", "/api/work-orders/**").authenticated()
                        .requestMatchers("/api/professional/**").authenticated()
                        .requestMatchers("/api/agent/**").authenticated()
                        // Preserve current PUBLIC APIs while the remaining manual checks are migrated.
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, error) -> writeProblem(response, 401, "authentication_required"))
                        .accessDeniedHandler((request, response, error) -> writeProblem(response, 403, "forbidden")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void writeProblem(HttpServletResponse response, int status, String code) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\"}");
    }
}
