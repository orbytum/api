package com.orbytum.api.configuration.security;

import com.orbytum.api.models.enums.Permissao;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    Permissao value();
}
