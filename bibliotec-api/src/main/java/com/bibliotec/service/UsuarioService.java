package com.bibliotec.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.bibliotec.dto.*;
import com.bibliotec.model.Usuario;
import com.bibliotec.repository.UsuarioRepository;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository repository,
            PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public UsuarioResponse registrar(RegistroUsuarioRequest datos) {
        String username = datos.username().toLowerCase(Locale.ROOT);
        if (datos.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "La contraseña no debe superar 72 bytes UTF-8");
        }
        if (repository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya existe");
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(datos.nombre().trim());
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(datos.password()));
        usuario.setRol("USER");
        usuario.setEstado(true);
        try {
            return UsuarioResponse.de(repository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException ex) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya existe");
        }
    }

    public UsuarioResponse buscarPorUsername(String username) {
        return UsuarioResponse.de(repository.findByUsername(username.toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    public List<UsuarioResponse> listar() {
        return repository.findAll().stream()
            .map(UsuarioResponse::de)
            .toList();
    }

    public UsuarioResponse buscar(Long id) {
        return UsuarioResponse.de(buscarEntidad(id));
    }

    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest datos) {
        Usuario usuario = buscarEntidad(id);
        usuario.setNombre(datos.nombre().trim());
        usuario.setEstado(datos.estado());
        usuario.setRol(datos.rol());
        return UsuarioResponse.de(repository.save(usuario));
    }

    public void eliminar(Long id) {
        Usuario usuario = buscarEntidad(id);
        try {
            repository.delete(usuario);
            repository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No se puede eliminar un usuario que tiene préstamos");
        }
    }

    private Usuario buscarEntidad(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
}
