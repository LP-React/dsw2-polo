package com.bibliotec.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import com.bibliotec.model.Libro;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    @Query("""
        SELECT l FROM Libro l
        WHERE (:titulo IS NULL OR LOWER(l.titulo) LIKE LOWER(CONCAT('%', :titulo, '%')))
          AND (:autor IS NULL OR LOWER(l.autor) LIKE LOWER(CONCAT('%', :autor, '%')))
          AND (:categoria IS NULL OR LOWER(l.categoria.nombre) LIKE LOWER(CONCAT('%', :categoria, '%')))
        ORDER BY l.titulo
        """)
    List<Libro> buscar(
        @Param("titulo") String titulo,
        @Param("autor") String autor,
        @Param("categoria") String categoria);
}
