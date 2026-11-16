package com.orbytum.api.util;

import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.Permissao;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    @Value("${jwt.secret:defaultSecretKeyForJWTTokenGenerationShouldBeLongEnough32Bytes}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(Base64.getEncoder().encodeToString(secret.getBytes()));
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails, AccessLevel accessLevel, List<Permissao> permissoes) {
        Map<String, Object> claims = new HashMap<>();
        String nivel = accessLevel != null ? accessLevel.getKey() : AccessLevel.USER.getKey();
        // REQ-009: payloads username, role e iat (o iat é definido em createToken)
        claims.put("username", userDetails.getUsername());
        claims.put("role", nivel);
        claims.put("accessLevel", nivel);
        claims.put("permissoes", permissoes != null ? permissoes.stream().map(Permissao::getKey).collect(Collectors.toList()) : Collections.emptyList());
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public AccessLevel extractAccessLevel(String token) {
        String key = extractAllClaims(token).get("accessLevel", String.class);
        return AccessLevel.fromKey(key);
    }

    public List<Permissao> extractPermissoes(String token) {
        List<String> permissoesKeys = extractAllClaims(token).get("permissoes", List.class);
        if (permissoesKeys == null) {
            return Collections.emptyList();
        }
        return permissoesKeys.stream()
                .map(Permissao::fromKey)
                .collect(Collectors.toList());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    /** Dados do link de assinatura da agenda de um grupo (REQ-069). */
    public record TokenCalendario(Long usuarioId, Long grupoId) {}

    /**
     * Gera o token do link de assinatura da agenda. É assinado com uma chave derivada,
     * diferente da usada no login, para que não possa ser usado como token de acesso à API.
     */
    public String gerarTokenCalendario(Long usuarioId, Long grupoId) {
        return Jwts.builder()
                .claim("uid", usuarioId)
                .claim("gid", grupoId)
                .issuedAt(new Date())
                .signWith(chaveCalendario())
                .compact();
    }

    /** Lança {@link io.jsonwebtoken.JwtException} se o token for inválido. */
    public TokenCalendario lerTokenCalendario(String token) {
        Claims claims = Jwts.parser().verifyWith(chaveCalendario()).build()
                .parseSignedClaims(token).getPayload();
        return new TokenCalendario(claims.get("uid", Long.class), claims.get("gid", Long.class));
    }

    private SecretKey chaveCalendario() {
        try {
            byte[] derivada = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(("calendario:" + secret).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(derivada);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
