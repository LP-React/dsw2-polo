package com.bibliotec.dto;

import jakarta.validation.constraints.NotNull;

public record RegistroPrestamoRequest(
    @NotNull(message = "Debe seleccionar un libro") Long idLibro
) {}
