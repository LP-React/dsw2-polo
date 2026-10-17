package com.bibliotec.config;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.bibliotec.model.Usuario;
import com.bibliotec.repository.UsuarioRepository;

@Configuration
public class DatosInicialesConfig {

    @Bean
    public ApplicationRunner prepararUsuarios(
            UsuarioRepository repository,
            PasswordEncoder encoder,
            @Value("${app.admin.username:admin}") String adminUsername,
            @Value("${app.admin.password:Admin12345}") String adminPassword) {
        return args -> {
            repository.findAll().stream()
                .filter(usuario -> usuario.getRol() == null || usuario.getRol().isBlank())
                .forEach(usuario -> {
                    usuario.setRol("USER");
                    repository.save(usuario);
                });

            String username = adminUsername.toLowerCase(Locale.ROOT).trim();
            if (!repository.existsByUsername(username)) {
                Usuario admin = new Usuario();
                admin.setNombre("Administrador");
                admin.setUsername(username);
                admin.setPassword(encoder.encode(adminPassword));
                admin.setRol("ADMIN");
                admin.setEstado(true);
                repository.save(admin);
            }
        };
    }
}
