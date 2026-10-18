package com.restaurant.api.domain.infra.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

  private final SecurityFilter securityFilter;

  @Value("${app.security.allowed-origins:${SECURITY_ORIGINS:http://localhost:3000,http://localhost:4200,http://localhost:5173,http://localhost:8080}}")
  private String allowedOrigins;

  public SecurityConfiguration(SecurityFilter securityFilter) {
    this.securityFilter = securityFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/auth/**").permitAll()
            .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/categories/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/categories/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT, "/categories/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/categories/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/sizes/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/sizes/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT, "/sizes/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/sizes/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/config/**").permitAll()
            .requestMatchers(HttpMethod.PUT, "/config/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PATCH, "/config/**").hasAnyRole("ADMIN", "WAITER")
            .requestMatchers(HttpMethod.POST,"/payment/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/products/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT, "/products/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/products/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/ingredient/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/ingredient/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT, "/ingredient/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/ingredient/**").hasRole("ADMIN")
            // Gestão de Equipe e Usuários
            .requestMatchers("/users/**").hasRole("ADMIN")
            // Mesas
            .requestMatchers(HttpMethod.GET, "/tables/**").hasAnyRole("ADMIN", "WAITER")
            .requestMatchers(HttpMethod.POST, "/tables/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT, "/tables/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PATCH, "/tables/**").hasAnyRole("ADMIN", "WAITER")
            // Cozinha / KDS
            .requestMatchers("/kitchen/**").hasAnyRole("ADMIN", "WAITER")
            // Pedidos: status, estimativas e cancelamento
            .requestMatchers(HttpMethod.PATCH, "/orders/*/status").hasAnyRole("ADMIN", "WAITER")
            .requestMatchers(HttpMethod.PATCH, "/orders/*/estimate").hasAnyRole("ADMIN", "WAITER")
            .requestMatchers(HttpMethod.PATCH, "/orders/*/cancel").authenticated()
            .requestMatchers(HttpMethod.GET, "/orders/me").authenticated()
            .requestMatchers(HttpMethod.GET, "/orders").hasAnyRole("ADMIN", "WAITER")
            .requestMatchers(HttpMethod.GET, "/orders/*").authenticated()
            .requestMatchers(HttpMethod.POST, "/orders").authenticated()
            .requestMatchers(HttpMethod.POST, "/orders/*/items").hasAnyRole("ADMIN", "WAITER")
            .requestMatchers(HttpMethod.GET, "/orders/*/items").authenticated()
            // Pagamentos cumulativos
            .requestMatchers(HttpMethod.POST, "/orders/*/payments").hasAnyRole("ADMIN", "WAITER")
            .requestMatchers(HttpMethod.GET, "/orders/*/payments").authenticated()
            // Relatórios e Fechamento de Caixa
            .requestMatchers("/reports/**").hasAnyRole("ADMIN", "WAITER")
            // Endereços do cliente
            .requestMatchers("/addresses/**").authenticated()
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    List<String> origins = Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();

    // Se for "*" ou contiver "*", permite todas as origens
    if (origins.contains("*")) {
      configuration.addAllowedOriginPattern("*");
    } else {
      configuration.setAllowedOrigins(origins);
    }

    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public AuthenticationManager authenticationManager(
      AuthenticationConfiguration authConfiguration
  ) throws Exception {
    return authConfiguration.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
