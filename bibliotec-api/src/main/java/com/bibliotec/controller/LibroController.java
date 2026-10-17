package com.bibliotec.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.bibliotec.model.Libro;
import com.bibliotec.service.LibroService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/libros")
public class LibroController {

    private final LibroService service;

    public LibroController(LibroService service) {
        this.service = service;
    }

    @GetMapping
    public List<Libro> listar(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String autor,
            @RequestParam(required = false) String categoria) {
        return service.listar(titulo, autor, categoria);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Libro> buscar(@PathVariable Long id) {

        Libro libro = service.buscar(id);

        if (libro == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(libro);
    }

    @PostMapping
    public Libro registrar(@Valid @RequestBody Libro libro) {
        return service.guardar(libro);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Libro> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody Libro datos) {

        Libro libro = service.buscar(id);

        if (libro == null) {
            return ResponseEntity.notFound().build();
        }

        libro.setTitulo(datos.getTitulo());
        libro.setAutor(datos.getAutor());
        libro.setIsbn(datos.getIsbn());
        libro.setAnioPublicacion(datos.getAnioPublicacion());
        libro.setCantidad(datos.getCantidad());
        libro.setEstado(datos.getEstado());
        libro.setCategoria(datos.getCategoria());

        return ResponseEntity.ok(service.guardar(libro));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        if (service.buscar(id) == null) {
            return ResponseEntity.notFound().build();
        }

        service.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
