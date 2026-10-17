package com.bibliotec.controller;

import java.security.Principal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bibliotec.dto.UsuarioResponse;
import com.bibliotec.service.UsuarioService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService service;

    public AuthController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public UsuarioResponse login(Principal principal) {
        return service.buscarPorUsername(principal.getName());
    }
}
