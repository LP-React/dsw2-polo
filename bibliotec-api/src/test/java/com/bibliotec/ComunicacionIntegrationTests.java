package com.bibliotec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import com.bibliotec.controller.ChatController;
import com.bibliotec.dto.MensajeChat;
import com.bibliotec.model.Categoria;
import com.bibliotec.model.Libro;
import com.bibliotec.repository.CategoriaRepository;
import com.bibliotec.repository.LibroRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:comunicacion;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    })
class ComunicacionIntegrationTests {

    @LocalServerPort int port;
    @Autowired LibroRepository libroRepository;
    @Autowired CategoriaRepository categoriaRepository;
    @Autowired ChatController chatController;

    private final HttpClient client = HttpClient.newHttpClient();
    private Long idLibro;

    @BeforeEach
    void prepararLibro() {
        libroRepository.deleteAll();
        categoriaRepository.deleteAll();

        Categoria categoria = new Categoria();
        categoria.setNombre("Tecnología");
        categoria.setEstado(true);
        categoriaRepository.save(categoria);

        Libro libro = new Libro();
        libro.setTitulo("Spring básico");
        libro.setCantidad(2);
        libro.setEstado(true);
        libro.setCategoria(categoria);
        idLibro = libroRepository.save(libro).getIdLibro();
    }

    @Test
    void publicaWsdlYRespondeConsultaSoap() throws Exception {
        HttpResponse<String> wsdl = client.send(
            HttpRequest.newBuilder(URI.create(
                "http://localhost:" + port + "/soap/libros.wsdl"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, wsdl.statusCode());
        assertTrue(wsdl.body().contains("consultarDisponibilidad"));

        String body = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                              xmlns:lib="http://bibliotec.com/soap/libros">
                <soapenv:Header/>
                <soapenv:Body>
                    <lib:consultarDisponibilidadRequest>
                        <lib:idLibro>%d</lib:idLibro>
                    </lib:consultarDisponibilidadRequest>
                </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(idLibro);

        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/soap"))
                .header("Content-Type", "text/xml;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Spring básico"));
        assertTrue(response.body().contains("<disponible>true</disponible>"));
    }

    @Test
    void chatCompletaLaFechaDelMensaje() {
        MensajeChat response = chatController.enviar(
            new MensajeChat("ana", "Hola", null));
        assertEquals("ana", response.usuario());
        assertEquals("Hola", response.mensaje());
        assertNotNull(response.fecha());
    }

    @Test
    void enviaYRecibeMensajePorWebSocket() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(
            new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompSession session = stompClient
            .connectAsync("ws://localhost:" + port + "/chat-websocket",
                new org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter() {})
            .get(5, TimeUnit.SECONDS);

        CompletableFuture<MensajeChat> mensajeRecibido = new CompletableFuture<>();
        session.subscribe("/topic/chat", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return MensajeChat.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                mensajeRecibido.complete((MensajeChat) payload);
            }
        });

        session.send("/app/chat.enviar", new MensajeChat("ana", "Hola", null));
        MensajeChat response = mensajeRecibido.get(5, TimeUnit.SECONDS);

        assertEquals("ana", response.usuario());
        assertEquals("Hola", response.mensaje());
        assertNotNull(response.fecha());
        session.disconnect();
        stompClient.stop();
    }

    @Test
    void permitePeticionesDelFrontendNextJs() throws Exception {
        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder(URI.create(
                "http://localhost:" + port + "/api/libros"))
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("http://localhost:3000",
            response.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
    }
}
