package com.bibliotec.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarUsuarioRequest(
    @NotBlank @Size(max = 100) String nombre,
    @NotNull Boolean estado,
    @NotBlank @Pattern(regexp = "USER|ADMIN",
        message = "El rol debe ser USER o ADMIN")
    String rol
) {}
