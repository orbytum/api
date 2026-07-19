package com.orbytum.api.configuration.security;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class RequiresPermissionAspect {

    private final AbacPermissionEvaluator abacPermissionEvaluator;

    @Around("@annotation(requiresPermission)")
    public Object intercept(ProceedingJoinPoint joinPoint, RequiresPermission requiresPermission) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

        if (!abacPermissionEvaluator.evaluateMethodPermission(authentication, method)) {
            throw new AccessDeniedException("Acesso negado: permissÃ£o " + requiresPermission.value().getKey() + " necessÃ¡ria");
        }

        return joinPoint.proceed();
    }
}
