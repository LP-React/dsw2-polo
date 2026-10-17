package com.bibliotec.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bibliotec.model.Prestamo;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    List<Prestamo> findByUsuarioUsernameOrderByFechaPrestamoDesc(String username);

    List<Prestamo> findAllByOrderByFechaPrestamoDesc();

    boolean existsByUsuarioIdUsuarioAndLibroIdLibroAndEstadoIn(
        Long idUsuario, Long idLibro, List<String> estados);
}
