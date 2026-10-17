package com.bibliotec.service;

import java.util.Locale;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import com.bibliotec.model.Usuario;
import com.bibliotec.repository.UsuarioRepository;

@Service
public class UsuarioDetailsService implements UserDetailsService {
    private final UsuarioRepository repository;

    public UsuarioDetailsService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = repository.findByUsername(username.toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new UsernameNotFoundException("Credenciales incorrectas"));
        return User.withUsername(usuario.getUsername())
            .password(usuario.getPassword())
            .roles(usuario.getRol() == null || usuario.getRol().isBlank()
                ? "USER" : usuario.getRol())
            .disabled(!Boolean.TRUE.equals(usuario.getEstado()))
            .build();
    }
}
