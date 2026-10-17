package com.bibliotec.dto;

import java.time.LocalDate;
import com.bibliotec.model.Prestamo;

public record PrestamoResponse(
    Long idPrestamo,
    Long idUsuario,
    String usuario,
    Long idLibro,
    String libro,
    LocalDate fechaPrestamo,
    LocalDate fechaVencimiento,
    LocalDate fechaDevolucion,
    String estado
) {
    public static PrestamoResponse de(Prestamo prestamo) {
        return new PrestamoResponse(
            prestamo.getIdPrestamo(),
            prestamo.getUsuario().getIdUsuario(),
            prestamo.getUsuario().getUsername(),
            prestamo.getLibro().getIdLibro(),
            prestamo.getLibro().getTitulo(),
            prestamo.getFechaPrestamo(),
            prestamo.getFechaVencimiento(),
            prestamo.getFechaDevolucion(),
            prestamo.getEstado()
        );
    }
}
