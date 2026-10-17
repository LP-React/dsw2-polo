package com.bibliotec;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.bibliotec.repository.UsuarioRepository;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.datasource.url=jdbc:h2:mem:usuarios;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop"})
class UsuarioIntegrationTests {
    @LocalServerPort int port;
    @Autowired UsuarioRepository repository;
    @Autowired PasswordEncoder encoder;
    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> request(String method, String path, String body, String credentials)
            throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (credentials != null) {
            builder.header("Authorization", "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }
        builder.header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void registroAutenticacionYAccesoExistente() throws Exception {
        String body = "{\"nombre\":\"Ana\",\"username\":\"AnaTest\",\"password\":\"Clave12345\"}";
        var response = request("POST", "/api/usuarios/registro", body, null);
        assertEquals(201, response.statusCode());
        assertFalse(response.body().contains("password"));
        assertFalse(response.body().contains("Clave12345"));
        var usuario = repository.findByUsername("anatest").orElseThrow();
        assertNotEquals("Clave12345", usuario.getPassword());
        assertTrue(encoder.matches("Clave12345", usuario.getPassword()));
        assertEquals(409, request("POST", "/api/usuarios/registro", body, null).statusCode());
        assertEquals(401, request("GET", "/api/usuarios/me", null, null).statusCode());
        assertEquals(401, request("GET", "/api/usuarios/me", null, "anatest:incorrecta").statusCode());
        assertEquals(401, request("GET", "/api/usuarios/me", null, "user:123456").statusCode());
        assertEquals(200, request("GET", "/api/usuarios/me", null, "AnaTest:Clave12345").statusCode());
        var login = request("POST", "/api/auth/login", null, "AnaTest:Clave12345");
        assertEquals(200, login.statusCode());
        assertTrue(login.body().contains("\"rol\":\"USER\""));
        for (String path : new String[]{"/api/categorias", "/api/libros"}) {
            assertEquals(401, request("GET", path, null, null).statusCode());
            assertEquals(200, request("GET", path, null, "anatest:Clave12345").statusCode());
        }
        usuario.setEstado(false);
        repository.saveAndFlush(usuario);
        assertEquals(401, request("GET", "/api/usuarios/me", null, "anatest:Clave12345").statusCode());
    }

    @Test
    void rechazaRegistroInvalido() throws Exception {
        assertEquals(400, request("POST", "/api/usuarios/registro",
            "{\"nombre\":\"\",\"username\":\"ab\",\"password\":\"123\"}", null).statusCode());
        String body = "{\"nombre\":\"Ana\",\"username\":\"largatest\",\"password\":\"" + "á".repeat(37) + "\"}";
        assertEquals(400, request("POST", "/api/usuarios/registro", body, null).statusCode());
        assertFalse(repository.existsByUsername("largatest"));
    }
}
