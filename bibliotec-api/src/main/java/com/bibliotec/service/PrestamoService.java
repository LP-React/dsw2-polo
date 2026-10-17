package com.bibliotec.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.bibliotec.dto.PrestamoResponse;
import com.bibliotec.model.Libro;
import com.bibliotec.model.Prestamo;
import com.bibliotec.model.Usuario;
import com.bibliotec.repository.LibroRepository;
import com.bibliotec.repository.PrestamoRepository;
import com.bibliotec.repository.UsuarioRepository;

@Service
public class PrestamoService {

    private static final String PENDIENTE = "PENDIENTE";
    private static final String PRESTADO = "PRESTADO";
    private static final String DEVUELTO = "DEVUELTO";
    private static final String RECHAZADO = "RECHAZADO";
    private static final int DIAS_PRESTAMO = 7;

    private final PrestamoRepository repository;
    private final LibroRepository libroRepository;
    private final UsuarioRepository usuarioRepository;

    public PrestamoService(
            PrestamoRepository repository,
            LibroRepository libroRepository,
            UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.libroRepository = libroRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public PrestamoResponse registrar(Long idLibro, String username) {
        Usuario usuario = usuarioRepository
            .findByUsername(username.toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Libro libro = libroRepository.findById(idLibro)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Libro no encontrado"));

        if (!Boolean.TRUE.equals(libro.getEstado()) ||
                libro.getCantidad() == null || libro.getCantidad() <= 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "El libro no se encuentra disponible");
        }

        if (repository.existsByUsuarioIdUsuarioAndLibroIdLibroAndEstadoIn(
                usuario.getIdUsuario(), idLibro, List.of(PENDIENTE, PRESTADO))) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Ya tiene una solicitud o préstamo activo para este libro");
        }

        Prestamo prestamo = new Prestamo();
        prestamo.setUsuario(usuario);
        prestamo.setLibro(libro);
        prestamo.setFechaPrestamo(LocalDate.now());
        prestamo.setEstado(PENDIENTE);

        return PrestamoResponse.de(repository.save(prestamo));
    }

    public List<PrestamoResponse> listarPorUsuario(String username) {
        return repository
            .findByUsuarioUsernameOrderByFechaPrestamoDesc(
                username.toLowerCase(Locale.ROOT))
            .stream()
            .map(PrestamoResponse::de)
            .toList();
    }

    public List<PrestamoResponse> listarTodos() {
        return repository.findAllByOrderByFechaPrestamoDesc().stream()
            .map(PrestamoResponse::de)
            .toList();
    }

    @Transactional
    public PrestamoResponse aprobar(Long idPrestamo) {
        Prestamo prestamo = buscarPrestamo(idPrestamo);
        if (!PENDIENTE.equals(prestamo.getEstado())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Solo se pueden aprobar préstamos pendientes");
        }

        Libro libro = prestamo.getLibro();
        if (!Boolean.TRUE.equals(libro.getEstado()) ||
                libro.getCantidad() == null || libro.getCantidad() <= 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "El libro no se encuentra disponible");
        }

        libro.setCantidad(libro.getCantidad() - 1);
        libroRepository.save(libro);
        prestamo.setEstado(PRESTADO);
        prestamo.setFechaVencimiento(LocalDate.now().plusDays(DIAS_PRESTAMO));
        return PrestamoResponse.de(repository.save(prestamo));
    }

    @Transactional
    public PrestamoResponse rechazar(Long idPrestamo) {
        Prestamo prestamo = buscarPrestamo(idPrestamo);
        if (!PENDIENTE.equals(prestamo.getEstado())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Solo se pueden rechazar préstamos pendientes");
        }
        prestamo.setEstado(RECHAZADO);
        return PrestamoResponse.de(repository.save(prestamo));
    }

    @Transactional
    public PrestamoResponse devolver(Long idPrestamo, String username) {
        Prestamo prestamo = buscarPrestamo(idPrestamo);

        if (!prestamo.getUsuario().getUsername().equalsIgnoreCase(username)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "El préstamo pertenece a otro usuario");
        }

        if (!PRESTADO.equals(prestamo.getEstado())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "El préstamo no está activo o ya fue devuelto");
        }

        Libro libro = prestamo.getLibro();
        int cantidadActual = libro.getCantidad() == null ? 0 : libro.getCantidad();
        libro.setCantidad(cantidadActual + 1);
        libroRepository.save(libro);

        prestamo.setFechaDevolucion(LocalDate.now());
        prestamo.setEstado(DEVUELTO);

        return PrestamoResponse.de(repository.save(prestamo));
    }

    private Prestamo buscarPrestamo(Long idPrestamo) {
        return repository.findById(idPrestamo)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Préstamo no encontrado"));
    }
}
