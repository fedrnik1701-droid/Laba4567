package org.example.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.example.repository.ReaderRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(ReaderRepository readers) {
        return email -> {
            var reader = readers.findByEmailIgnoreCase(email)
                    .filter(r -> r.getPassword() != null)
                    .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
            return User.withUsername(reader.getEmail()).password(reader.getPassword())
                    .roles(reader.getRole().name()).disabled(!Boolean.TRUE.equals(reader.getActive()))
                    .build();
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, exception) -> {
            response.setHeader("WWW-Authenticate", "Basic realm=\"library\"");
            error(response, 401, "Authentication required or invalid credentials");
        };
        return http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/books/**", "/api/categories/**",
                                "/api/readers/**", "/api/loans/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/business/extend",
                                "/api/business/pay-fine").authenticated()
                        .requestMatchers("/api/**").hasRole("LIBRARIAN")
                        .anyRequest().denyAll())
                .httpBasic(basic -> basic.authenticationEntryPoint(unauthorized))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler((request, response, exception) ->
                                error(response, 403, "Access denied or invalid CSRF token")))
                .requestCache(cache -> cache.disable())
                .logout(logout -> logout.disable())
                .build(); // CSRF включён по умолчанию; токен связан с JSESSIONID.
    }

    private static void error(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
    }
}
