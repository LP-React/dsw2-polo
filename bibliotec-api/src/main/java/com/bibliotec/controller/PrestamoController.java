package com.bibliotec.controller;

import java.security.Principal;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.bibliotec.dto.PrestamoResponse;
import com.bibliotec.dto.RegistroPrestamoRequest;
import com.bibliotec.service.PrestamoService;

@RestController
@RequestMapping("/api/prestamos")
public class PrestamoController {

    private final PrestamoService service;

    public PrestamoController(PrestamoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrestamoResponse registrar(
            @Valid @RequestBody RegistroPrestamoRequest datos,
            Principal principal) {
        return service.registrar(datos.idLibro(), principal.getName());
    }

    @GetMapping("/mis-prestamos")
    public List<PrestamoResponse> listarMios(Principal principal) {
        return service.listarPorUsuario(principal.getName());
    }

    @GetMapping
    public List<PrestamoResponse> listarTodos() {
        return service.listarTodos();
    }

    @PutMapping("/{id}/aprobar")
    public PrestamoResponse aprobar(@PathVariable Long id) {
        return service.aprobar(id);
    }

    @PutMapping("/{id}/rechazar")
    public PrestamoResponse rechazar(@PathVariable Long id) {
        return service.rechazar(id);
    }

    @PutMapping("/{id}/devolver")
    public PrestamoResponse devolver(
            @PathVariable Long id,
            Principal principal) {
        return service.devolver(id, principal.getName());
    }
}
