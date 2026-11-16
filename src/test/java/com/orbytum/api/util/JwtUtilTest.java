package com.orbytum.api.util;

import com.orbytum.api.models.enums.AccessLevel;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private UserDetails usuario;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "chave-de-teste-com-mais-de-trinta-e-dois-bytes");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3_600_000L);
        usuario = User.withUsername("pesquisador@teste.com").password("x").authorities(List.of()).build();
    }

    /** JSON do payload (segunda parte do JWT), sem depender de biblioteca de JSON. */
    private String payload(String token) {
        return new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
    }

    @Test
    void tokenDeAcessoContemUsernameRoleEIat() {
        String token = jwtUtil.generateToken(usuario, AccessLevel.USER, List.of());

        String json = payload(token);

        assertTrue(json.contains("\"username\":\"pesquisador@teste.com\""), json);
        assertTrue(json.contains("\"role\":\"user\""), json);
        assertTrue(json.matches(".*\"iat\":\\d+.*"), json);
        assertEquals("pesquisador@teste.com", jwtUtil.extractEmail(token));
        assertEquals(AccessLevel.USER, jwtUtil.extractAccessLevel(token));
    }

    @Test
    void roleRefleteONivelDeAcessoDoAdministrador() {
        String token = jwtUtil.generateToken(usuario, AccessLevel.ADMIN, List.of());

        assertTrue(payload(token).contains("\"role\":\"admin\""));
    }

    @Test
    void tokenDeCalendarioPreservaUsuarioEGrupo() {
        String token = jwtUtil.gerarTokenCalendario(42L, 7L);

        JwtUtil.TokenCalendario dados = jwtUtil.lerTokenCalendario(token);

        assertEquals(42L, dados.usuarioId());
        assertEquals(7L, dados.grupoId());
    }

    @Test
    void tokenDeCalendarioNaoServeComoTokenDeAcesso() {
        String tokenCalendario = jwtUtil.gerarTokenCalendario(42L, 7L);

        assertThrows(JwtException.class, () -> jwtUtil.extractEmail(tokenCalendario));
    }

    @Test
    void tokenDeAcessoNaoServeComoLinkDeCalendario() {
        String tokenAcesso = jwtUtil.generateToken(usuario, AccessLevel.USER, List.of());

        assertThrows(JwtException.class, () -> jwtUtil.lerTokenCalendario(tokenAcesso));
    }

    @Test
    void tokenDeCalendarioAdulteradoEhRejeitado() {
        String token = jwtUtil.gerarTokenCalendario(42L, 7L);
        String adulterado = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThrows(JwtException.class, () -> jwtUtil.lerTokenCalendario(adulterado));
    }
}
