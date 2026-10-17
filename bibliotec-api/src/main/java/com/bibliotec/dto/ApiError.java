package com.bibliotec.dto;

import java.time.LocalDateTime;

public record ApiError(
    LocalDateTime fecha,
    int estado,
    String error,
    String mensaje,
    String ruta
) {}
