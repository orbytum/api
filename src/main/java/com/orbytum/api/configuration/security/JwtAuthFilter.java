package com.orbytum.api.configuration.security;

import com.orbytum.api.models.entity.CredenciaisLogin;
import com.orbytum.api.repository.CredenciaisLoginRepository;
import com.orbytum.api.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CredenciaisLoginRepository credenciaisLoginRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final String email = jwtUtil.extractEmail(jwt);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            CredenciaisLogin credenciais = credenciaisLoginRepository.findByEmail(email).orElse(null);

            if (credenciais != null && credenciais.isAtivo()) {
                UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                        credenciais.getEmail(),
                        credenciais.getSenha(),
                        java.util.Collections.emptyList()
                );

                if (jwtUtil.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, jwt, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
