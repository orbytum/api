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
        claims.put("accessLevel", accessLevel != null ? accessLevel.getKey() : AccessLevel.USER.getKey());
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
}
