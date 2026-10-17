package com.bibliotec.config;

import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.bibliotec.model.Libro;
import com.bibliotec.service.LibroService;

@Endpoint
public class LibroSoapEndpoint {

    public static final String NAMESPACE_URI = "http://bibliotec.com/soap/libros";

    private final LibroService service;

    public LibroSoapEndpoint(LibroService service) {
        this.service = service;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "consultarDisponibilidadRequest")
    @ResponsePayload
    public Element consultarDisponibilidad(@RequestPayload Element request) throws Exception {
        String textoId = request
            .getElementsByTagNameNS(NAMESPACE_URI, "idLibro")
            .item(0)
            .getTextContent();
        Long idLibro = Long.valueOf(textoId);
        Libro libro = service.buscar(idLibro);

        Document document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .newDocument();
        Element response = document.createElementNS(
            NAMESPACE_URI, "consultarDisponibilidadResponse");
        document.appendChild(response);

        agregar(document, response, "idLibro", idLibro.toString());
        if (libro == null) {
            agregar(document, response, "titulo", "");
            agregar(document, response, "cantidad", "0");
            agregar(document, response, "disponible", "false");
            agregar(document, response, "mensaje", "Libro no encontrado");
            return response;
        }

        int cantidad = libro.getCantidad() == null ? 0 : libro.getCantidad();
        boolean disponible = Boolean.TRUE.equals(libro.getEstado()) && cantidad > 0;
        agregar(document, response, "titulo", libro.getTitulo());
        agregar(document, response, "cantidad", String.valueOf(cantidad));
        agregar(document, response, "disponible", String.valueOf(disponible));
        agregar(document, response, "mensaje",
            disponible ? "Libro disponible" : "Libro no disponible");
        return response;
    }

    private void agregar(Document document, Element padre, String nombre, String valor) {
        Element elemento = document.createElementNS(NAMESPACE_URI, nombre);
        elemento.setTextContent(valor);
        padre.appendChild(elemento);
    }
}
