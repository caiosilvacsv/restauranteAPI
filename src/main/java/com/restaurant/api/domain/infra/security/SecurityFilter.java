package com.restaurant.api.domain.infra.security;

import com.restaurant.api.domain.entity.User;
import com.restaurant.api.domain.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

  private final TokenService tokenService;
  private final UserRepository userRepository;

  public SecurityFilter(TokenService tokenService, UserRepository userRepository) {
    this.tokenService = tokenService;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    String token = this.recoverToken(request);
    if (token != null) {
      String login = tokenService.validateToken(token);
      if (login != null && !login.isBlank()) {
        User user = userRepository.findByLogin(login);
        if (user != null && user.isEnabled()) {
          var authentication = new UsernamePasswordAuthenticationToken(
              user,
              null,
              user.getAuthorities()
          );
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      }
    }
    filterChain.doFilter(request, response);
  }

  private String recoverToken(HttpServletRequest request) {
    var authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer "))
      return authHeader.replace("Bearer ", "");
    return null;
  }
}
