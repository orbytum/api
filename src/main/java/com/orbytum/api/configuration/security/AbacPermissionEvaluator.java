package com.orbytum.api.configuration.security;

import com.orbytum.api.models.enums.AccessLevel;
import com.orbytum.api.models.enums.Permissao;
import com.orbytum.api.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AbacPermissionEvaluator {

    private final JwtUtil jwtUtil;

    public boolean hasPermission(Authentication authentication, Object targetId, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object credentials = authentication.getCredentials();
        if (credentials == null) {
            return false;
        }

        String token = credentials.toString();
        AccessLevel accessLevel = jwtUtil.extractAccessLevel(token);

        if (accessLevel == AccessLevel.ADMIN) {
            return true;
        }

        if (permission instanceof Permissao permissaoRequerida) {
            List<Permissao> permissoes = jwtUtil.extractPermissoes(token);
            return permissoes.contains(permissaoRequerida);
        }

        return false;
    }

    public boolean hasPermission(Authentication authentication, Object targetDomainObject, String targetType, Object permission) {
        return hasPermission(authentication, targetDomainObject, permission);
    }

    public boolean evaluateMethodPermission(Authentication authentication, Method method) {
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);
        if (annotation == null) {
            return true;
        }
        return hasPermission(authentication, null, annotation.value());
    }
}
