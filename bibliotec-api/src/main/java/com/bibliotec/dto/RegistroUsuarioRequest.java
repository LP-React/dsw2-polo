package com.bibliotec.dto;

import jakarta.validation.constraints.*;

public record RegistroUsuarioRequest(
    @NotBlank @Size(max = 100) String nombre,
    @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,50}",
        message = "El usuario debe tener entre 3 y 50 letras, números, puntos, guiones o guiones bajos")
    String username,
    @NotBlank @Size(min = 8, max = 72) String password
) {}
