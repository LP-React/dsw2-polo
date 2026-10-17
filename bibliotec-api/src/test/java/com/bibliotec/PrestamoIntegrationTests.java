package com.bibliotec;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.bibliotec.model.Categoria;
import com.bibliotec.model.Libro;
import com.bibliotec.model.Usuario;
import com.bibliotec.repository.CategoriaRepository;
import com.bibliotec.repository.LibroRepository;
import com.bibliotec.repository.PrestamoRepository;
import com.bibliotec.repository.UsuarioRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:prestamos;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    })
class PrestamoIntegrationTests {

    @LocalServerPort int port;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired LibroRepository libroRepository;
    @Autowired CategoriaRepository categoriaRepository;
    @Autowired PrestamoRepository prestamoRepository;
    @Autowired PasswordEncoder encoder;

    private final HttpClient client = HttpClient.newHttpClient();
    private Long idLibro;
    private Long idPrestamo;

    @BeforeEach
    void prepararDatos() {
        prestamoRepository.deleteAll();
        libroRepository.deleteAll();
        categoriaRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario usuario = new Usuario();
        usuario.setNombre("Ana");
        usuario.setUsername("ana");
        usuario.setPassword(encoder.encode("Clave12345"));
        usuario.setRol("USER");
        usuario.setEstado(true);
        usuarioRepository.save(usuario);

        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setUsername("admin");
        admin.setPassword(encoder.encode("Admin12345"));
        admin.setRol("ADMIN");
        admin.setEstado(true);
        usuarioRepository.save(admin);

        Categoria categoria = new Categoria();
        categoria.setNombre("Novela");
        categoria.setEstado(true);
        categoriaRepository.save(categoria);

        Libro libro = new Libro();
        libro.setTitulo("El principito");
        libro.setCantidad(1);
        libro.setEstado(true);
        libro.setCategoria(categoria);
        idLibro = libroRepository.save(libro).getIdLibro();
    }

    private HttpResponse<String> request(
            String method, String path, String body, String credencialesUsuario)
            throws Exception {
        String credenciales = Base64.getEncoder().encodeToString(
            credencialesUsuario.getBytes(StandardCharsets.UTF_8));
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + path))
            .header("Authorization", "Basic " + credenciales)
            .header("Content-Type", "application/json");
        builder.method(method, body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void registraListaYDevuelvePrestamo() throws Exception {
        HttpResponse<String> registro = request(
            "POST", "/api/prestamos", "{\"idLibro\":" + idLibro + "}",
            "ana:Clave12345");
        assertEquals(201, registro.statusCode());
        assertEquals(1, libroRepository.findById(idLibro).orElseThrow().getCantidad());
        assertEquals("PENDIENTE", prestamoRepository.findAll().get(0).getEstado());

        idPrestamo = prestamoRepository.findAll().get(0).getIdPrestamo();
        HttpResponse<String> aprobacion = request(
            "PUT", "/api/prestamos/" + idPrestamo + "/aprobar", null,
            "admin:Admin12345");
        assertEquals(200, aprobacion.statusCode());
        assertEquals(0, libroRepository.findById(idLibro).orElseThrow().getCantidad());
        assertEquals("PRESTADO", prestamoRepository.findById(idPrestamo).orElseThrow().getEstado());
        assertEquals(java.time.LocalDate.now().plusDays(7),
            prestamoRepository.findById(idPrestamo).orElseThrow().getFechaVencimiento());

        assertEquals(200,
            request("GET", "/api/prestamos/mis-prestamos", null,
                "ana:Clave12345").statusCode());

        assertEquals(200,
            request("PUT", "/api/prestamos/" + idPrestamo + "/devolver", null,
                "ana:Clave12345")
                .statusCode());
        assertEquals(1, libroRepository.findById(idLibro).orElseThrow().getCantidad());

        assertEquals(409,
            request("PUT", "/api/prestamos/" + idPrestamo + "/devolver", null,
                "ana:Clave12345")
                .statusCode());
    }

    @Test
    void rechazaPrestamoCuandoNoHayDisponibilidad() throws Exception {
        Libro libro = libroRepository.findById(idLibro).orElseThrow();
        libro.setCantidad(0);
        libroRepository.saveAndFlush(libro);

        assertEquals(409, request(
            "POST", "/api/prestamos", "{\"idLibro\":" + idLibro + "}",
            "ana:Clave12345")
            .statusCode());
    }

    @Test
    void usuarioNoPuedeAprobarPrestamos() throws Exception {
        HttpResponse<String> registro = request(
            "POST", "/api/prestamos", "{\"idLibro\":" + idLibro + "}",
            "ana:Clave12345");
        assertEquals(201, registro.statusCode());
        idPrestamo = prestamoRepository.findAll().get(0).getIdPrestamo();

        HttpResponse<String> response = request(
            "PUT", "/api/prestamos/" + idPrestamo + "/aprobar", null,
            "ana:Clave12345");
        assertEquals(403, response.statusCode());
        assertEquals("PENDIENTE",
            prestamoRepository.findById(idPrestamo).orElseThrow().getEstado());
    }
}
