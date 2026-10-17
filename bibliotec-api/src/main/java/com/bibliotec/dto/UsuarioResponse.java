package com.bibliotec.dto;

import com.bibliotec.model.Usuario;


public record UsuarioResponse(
        Long idUsuario,
        String nombre,
        String username,
        String rol,
        Boolean estado) {
    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getIdUsuario(), usuario.getNombre(),
            usuario.getUsername(), usuario.getRol(), usuario.getEstado());
    }
}
