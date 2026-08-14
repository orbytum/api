package com.orbytum.api.controller;

import com.orbytum.api.fachada.GrupoFachada;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/grupos")
@RequiredArgsConstructor
public class GrupoController {

    private final GrupoFachada grupoFachada;

}
