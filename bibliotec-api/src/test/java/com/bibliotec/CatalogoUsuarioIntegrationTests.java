package com.bibliotec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.bibliotec.model.Usuario;
import com.bibliotec.repository.CategoriaRepository;
import com.bibliotec.repository.LibroRepository;
import com.bibliotec.repository.PrestamoRepository;
import com.bibliotec.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:catalogo;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    })
class CatalogoUsuarioIntegrationTests {

    @LocalServerPort int port;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired LibroRepository libroRepository;
    @Autowired CategoriaRepository categoriaRepository;
    @Autowired PrestamoRepository prestamoRepository;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper objectMapper;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void prepararUsuarios() {
        prestamoRepository.deleteAll();
        libroRepository.deleteAll();
        categoriaRepository.deleteAll();
        usuarioRepository.deleteAll();
        guardarUsuario("Administrador", "admin", "Admin12345", "ADMIN");
        guardarUsuario("Ana", "ana", "Clave12345", "USER");
    }

    @Test
    void administradorGestionaCatalogoYUsuarioBuscaLibros() throws Exception {
        HttpResponse<String> categoria = request("POST", "/api/categorias",
            "{\"nombre\":\"Tecnología\",\"descripcion\":\"Programación\",\"estado\":true}",
            "admin:Admin12345");
        assertEquals(200, categoria.statusCode());
        long idCategoria = json(categoria).get("idCategoria").asLong();

        String libroJson = """
            {"titulo":"Spring básico","autor":"Ana Pérez","isbn":"ABC-1",
             "anioPublicacion":2025,"cantidad":3,"estado":true,
             "categoria":{"idCategoria":%d}}
            """.formatted(idCategoria);
        HttpResponse<String> libro = request("POST", "/api/libros", libroJson,
            "admin:Admin12345");
        assertEquals(200, libro.statusCode());
        long idLibro = json(libro).get("idLibro").asLong();

        HttpResponse<String> busqueda = request("GET",
            "/api/libros?titulo=spring&autor=pérez&categoria=tec", null,
            "ana:Clave12345");
        assertEquals(200, busqueda.statusCode());
        assertTrue(busqueda.body().contains("Spring básico"));

        assertEquals(403, request("POST", "/api/libros", libroJson,
            "ana:Clave12345").statusCode());

        String actualizado = libroJson.replace("Spring básico", "Spring MVC");
        assertEquals(200, request("PUT", "/api/libros/" + idLibro,
            actualizado, "admin:Admin12345").statusCode());
        assertEquals(204, request("DELETE", "/api/libros/" + idLibro, null,
            "admin:Admin12345").statusCode());
        assertEquals(204, request("DELETE", "/api/categorias/" + idCategoria, null,
            "admin:Admin12345").statusCode());
    }

    @Test
    void administradorGestionaUsuariosSinExponerPasswords() throws Exception {
        HttpResponse<String> registro = request("POST", "/api/usuarios/registro",
            "{\"nombre\":\"Luis\",\"username\":\"luis\",\"password\":\"Clave12345\"}",
            null);
        assertEquals(201, registro.statusCode());
        long id = json(registro).get("idUsuario").asLong();

        HttpResponse<String> listado = request("GET", "/api/usuarios", null,
            "admin:Admin12345");
        assertEquals(200, listado.statusCode());
        assertTrue(listado.body().contains("luis"));
        assertFalse(listado.body().contains("Clave12345"));

        HttpResponse<String> actualizado = request("PUT", "/api/usuarios/" + id,
            "{\"nombre\":\"Luis Pérez\",\"estado\":true,\"rol\":\"ADMIN\"}",
            "admin:Admin12345");
        assertEquals(200, actualizado.statusCode());
        assertEquals("ADMIN", json(actualizado).get("rol").asText());

        assertEquals(204, request("DELETE", "/api/usuarios/" + id, null,
            "admin:Admin12345").statusCode());
        assertFalse(usuarioRepository.existsByUsername("luis"));
    }

    private void guardarUsuario(
            String nombre, String username, String password, String rol) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setUsername(username);
        usuario.setPassword(encoder.encode(password));
        usuario.setRol(rol);
        usuario.setEstado(true);
        usuarioRepository.save(usuario);
    }

    private JsonNode json(HttpResponse<String> response) throws Exception {
        return objectMapper.readTree(response.body());
    }

    private HttpResponse<String> request(
            String method, String path, String body, String credentials)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
            URI.create("http://localhost:" + port + path));
        if (credentials != null) {
            builder.header("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }
        builder.header("Content-Type", "application/json");
        builder.method(method, body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
