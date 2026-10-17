package com.bibliotec.service;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.bibliotec.model.Categoria;
import com.bibliotec.model.Libro;
import com.bibliotec.repository.CategoriaRepository;
import com.bibliotec.repository.LibroRepository;

@Service
public class LibroService {

    private final LibroRepository repository;
    private final CategoriaRepository categoriaRepository;

    public LibroService(
            LibroRepository repository,
            CategoriaRepository categoriaRepository) {
        this.repository = repository;
        this.categoriaRepository = categoriaRepository;
    }

    public List<Libro> listar(String titulo, String autor, String categoria) {
        return repository.buscar(
            textoOpcional(titulo),
            textoOpcional(autor),
            textoOpcional(categoria));
    }

    public Libro buscar(Long id) {
        return repository.findById(id).orElse(null);
    }

    public Libro guardar(Libro libro) {

        if (libro.getCategoria() == null ||
            libro.getCategoria().getIdCategoria() == null) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debe seleccionar una categoría"
            );
        }

        Long idCategoria = libro.getCategoria().getIdCategoria();

        Categoria categoria = categoriaRepository.findById(idCategoria)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La categoría no existe"
            ));

        libro.setCategoria(categoria);

        return repository.save(libro);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    private String textoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
