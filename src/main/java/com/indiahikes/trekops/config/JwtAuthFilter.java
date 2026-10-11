package com.indiahikes.trekops.config;

import com.indiahikes.trekops.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads "Authorization: Bearer token". A valid token becomes the authenticated user for this
 * request only. A missing or bad token leaves the request unauthenticated, and Spring Security then
 * answers 401 on protected paths. Nothing is stored between requests.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;

  public JwtAuthFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith(BEARER_PREFIX)) {
      try {
        JwtService.TokenClaims claims =
            jwtService.parseToken(header.substring(BEARER_PREFIX.length()));
        var authority = new SimpleGrantedAuthority("ROLE_" + claims.role().name());
        var authentication =
            new UsernamePasswordAuthenticationToken(claims.username(), null, List.of(authority));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (JwtException e) {
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }
}
