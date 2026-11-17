package com.uniremignton.barberia.service;

import com.uniremignton.barberia.model.dao.CitaDAO;
import com.uniremignton.barberia.model.dao.CitaServicioDAO;
import com.uniremignton.barberia.model.dao.PagoDAO;
import com.uniremignton.barberia.model.vo.BarberoServicioVO;
import com.uniremignton.barberia.model.vo.BarberoVO;
import com.uniremignton.barberia.model.vo.CitaServicioVO;
import com.uniremignton.barberia.model.vo.CitaVO;
import com.uniremignton.barberia.model.vo.ClienteVO;
import com.uniremignton.barberia.model.vo.EstadoBarbero;
import com.uniremignton.barberia.model.vo.EstadoCita;
import com.uniremignton.barberia.model.vo.EstadoCliente;
import com.uniremignton.barberia.model.vo.EstadoPago;
import com.uniremignton.barberia.service.exception.RecursoNoEncontradoException;
import com.uniremignton.barberia.service.exception.ReglaNegocioException;
import com.uniremignton.barberia.service.exception.ValidacionException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestiona las citas. Garantiza que un barbero no tenga citas superpuestas: valida su horario de
 * trabajo y el cruce con otras citas vigentes. El registro y la reprogramación se ejecutan en
 * aislamiento SERIALIZABLE para que dos reservas simultáneas sobre el mismo hueco no puedan
 * confirmarse a la vez (la segunda falla con una {@link ReglaNegocioException}).
 */
@Service
@Transactional(readOnly = true)
public class CitaService {

    /** Estados en los que la cita sigue abierta (aún no finalizada ni cerrada). */
    public static final Set<EstadoCita> ESTADOS_VIGENTES = Collections.unmodifiableSet(
            EnumSet.of(EstadoCita.PROGRAMADA, EstadoCita.CONFIRMADA, EstadoCita.EN_PROCESO));

    private static final Map<EstadoCita, Set<EstadoCita>> TRANSICIONES = Map.of(
            EstadoCita.PROGRAMADA, EnumSet.of(
                    EstadoCita.CONFIRMADA, EstadoCita.EN_PROCESO, EstadoCita.CANCELADA, EstadoCita.NO_ASISTIO),
            EstadoCita.CONFIRMADA, EnumSet.of(
                    EstadoCita.EN_PROCESO, EstadoCita.CANCELADA, EstadoCita.NO_ASISTIO),
            EstadoCita.EN_PROCESO, EnumSet.of(EstadoCita.COMPLETADA, EstadoCita.CANCELADA),
            EstadoCita.COMPLETADA, EnumSet.noneOf(EstadoCita.class),
            EstadoCita.CANCELADA, EnumSet.noneOf(EstadoCita.class),
            EstadoCita.NO_ASISTIO, EnumSet.noneOf(EstadoCita.class));

    private final CitaDAO citaDAO;
    private final CitaServicioDAO citaServicioDAO;
    private final PagoDAO pagoDAO;
    private final ClienteService clienteService;
    private final BarberoService barberoService;
    private final ServicioService servicioService;
    private final BarberoServicioService barberoServicioService;
    private final HorarioBarberoService horarioService;

    public CitaService(
            CitaDAO citaDAO,
            CitaServicioDAO citaServicioDAO,
            PagoDAO pagoDAO,
            ClienteService clienteService,
            BarberoService barberoService,
            ServicioService servicioService,
            BarberoServicioService barberoServicioService,
            HorarioBarberoService horarioService) {
        this.citaDAO = citaDAO;
        this.citaServicioDAO = citaServicioDAO;
        this.pagoDAO = pagoDAO;
        this.clienteService = clienteService;
        this.barberoService = barberoService;
        this.servicioService = servicioService;
        this.barberoServicioService = barberoServicioService;
        this.horarioService = horarioService;
    }

    /** Registra una cita PROGRAMADA (por defecto) o CONFIRMADA, sin cruces con otras citas del barbero. */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CitaVO registrar(CitaVO cita) {
        return protegerAgenda(() -> registrarCita(cita));
    }

    /**
     * Actualiza la cita (reprogramación, cambio de cliente o barbero, observaciones y/o estado).
     * Los servicios y pagos de la cita se gestionan con sus propios Service.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CitaVO actualizar(CitaVO cita) {
        return protegerAgenda(() -> actualizarCita(cita));
    }

    /** Cambia únicamente el estado respetando las transiciones permitidas. */
    @Transactional
    public CitaVO cambiarEstado(long idCita, EstadoCita nuevoEstado) {
        CitaVO actual = buscarPorId(idCita);
        return actualizarCita(new CitaVO(
                actual.idCita(), actual.idCliente(), actual.idBarbero(), actual.fechaHoraInicio(),
                actual.fechaHoraFin(), Validador.requerido(nuevoEstado, "estado"),
                actual.observaciones(), null, null));
    }

    public CitaVO buscarPorId(long idCita) {
        Validador.idPositivo(idCita, "idCita");
        try {
            return citaDAO.buscarPorId(idCita);
        } catch (EmptyResultDataAccessException e) {
            throw new RecursoNoEncontradoException("Cita", idCita);
        }
    }

    /** Devuelve la cita solo si sigue vigente (PROGRAMADA, CONFIRMADA o EN_PROCESO). */
    public CitaVO buscarVigente(long idCita) {
        CitaVO cita = buscarPorId(idCita);
        if (!ESTADOS_VIGENTES.contains(cita.estado())) {
            throw new ReglaNegocioException(
                    "La cita " + idCita + " está " + cita.estado() + " y ya no admite cambios");
        }
        return cita;
    }

    public List<CitaVO> listar() {
        return citaDAO.listar();
    }

    public List<CitaVO> listarPorCliente(long idCliente) {
        clienteService.buscarPorId(idCliente);
        return citaDAO.listarPorCliente(idCliente);
    }

    public List<CitaVO> listarPorBarbero(long idBarbero) {
        barberoService.buscarPorId(idBarbero);
        return citaDAO.listarPorBarbero(idBarbero);
    }

    /** Citas cuyo inicio está en el rango [desde, hasta). */
    public List<CitaVO> listarPorRango(LocalDateTime desde, LocalDateTime hasta) {
        Validador.requerido(desde, "desde");
        Validador.requerido(hasta, "hasta");
        if (!desde.isBefore(hasta)) {
            throw new ValidacionException("La fecha inicial debe ser anterior a la fecha final");
        }
        return citaDAO.listarPorRango(desde, hasta);
    }

    @Transactional
    public void eliminar(long idCita) {
        CitaVO cita = buscarPorId(idCita);
        if (cita.estado() == EstadoCita.COMPLETADA) {
            throw new ReglaNegocioException("No se puede eliminar una cita COMPLETADA");
        }
        try {
            if (!citaDAO.eliminar(idCita)) {
                throw new RecursoNoEncontradoException("Cita", idCita);
            }
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException(
                    "No se puede eliminar la cita porque tiene servicios o pagos asociados; "
                            + "cámbiela al estado CANCELADA",
                    e);
        }
    }

    /**
     * Verifica que el barbero de la cita ofrezca todos los servicios indicados y que la suma de sus
     * duraciones (duración x cantidad) quepa en el intervalo de la cita.
     */
    public void validarServiciosEnCita(CitaVO cita, List<CitaServicioVO> detalles) {
        if (detalles.isEmpty()) {
            return;
        }
        Set<Long> ofrecidos = new HashSet<>();
        for (BarberoServicioVO relacion : barberoServicioService.listarPorBarbero(cita.idBarbero())) {
            ofrecidos.add(relacion.idServicio());
        }
        long minutosRequeridos = 0;
        for (CitaServicioVO detalle : detalles) {
            if (!ofrecidos.contains(detalle.idServicio())) {
                throw new ReglaNegocioException(
                        "El barbero " + cita.idBarbero() + " no ofrece el servicio " + detalle.idServicio());
            }
            minutosRequeridos += (long) servicioService.buscarPorId(detalle.idServicio()).duracionMinutos()
                    * detalle.cantidad();
        }
        long minutosCita = Duration.between(cita.fechaHoraInicio(), cita.fechaHoraFin()).toMinutes();
        if (minutosRequeridos > minutosCita) {
            throw new ReglaNegocioException("Los servicios requieren " + minutosRequeridos
                    + " minutos y la cita solo dura " + minutosCita);
        }
    }

    private CitaVO registrarCita(CitaVO cita) {
        Validador.requerido(cita, "cita");
        EstadoCita estado = cita.estado() != null ? cita.estado() : EstadoCita.PROGRAMADA;
        if (estado != EstadoCita.PROGRAMADA && estado != EstadoCita.CONFIRMADA) {
            throw new ReglaNegocioException("Una cita nueva solo puede registrarse como PROGRAMADA o CONFIRMADA");
        }
        CitaVO nueva = normalizar(cita, null, estado);
        validarCliente(nueva.idCliente());
        validarBarbero(nueva.idBarbero());
        validarInicioNoPasado(nueva.fechaHoraInicio());
        validarAgenda(nueva, null);
        return citaDAO.crear(nueva);
    }

    private CitaVO actualizarCita(CitaVO cita) {
        Validador.requerido(cita, "cita");
        long id = Validador.idPositivo(cita.idCita(), "idCita");
        CitaVO actual = buscarPorId(id);
        EstadoCita estado = Validador.requerido(cita.estado(), "estado");
        validarTransicion(actual.estado(), estado);
        CitaVO nueva = normalizar(cita, id, estado);

        boolean cambioCliente = !nueva.idCliente().equals(actual.idCliente());
        boolean cambioBarbero = !nueva.idBarbero().equals(actual.idBarbero());
        boolean cambioInicio = !nueva.fechaHoraInicio().equals(aMinutos(actual.fechaHoraInicio()));
        boolean cambioFin = !nueva.fechaHoraFin().equals(aMinutos(actual.fechaHoraFin()));

        if (cambioCliente) {
            validarCliente(nueva.idCliente());
        }
        if (cambioBarbero) {
            validarBarbero(nueva.idBarbero());
        }
        if (cambioInicio) {
            validarInicioNoPasado(nueva.fechaHoraInicio());
        }

        List<CitaServicioVO> detalles = citaServicioDAO.listarPorCita(id);
        boolean ocupaAgenda = estado != EstadoCita.CANCELADA && estado != EstadoCita.NO_ASISTIO;
        if ((cambioBarbero || cambioInicio || cambioFin) && ocupaAgenda) {
            validarServiciosEnCita(nueva, detalles);
            validarAgenda(nueva, id);
        }
        if (estado != actual.estado()) {
            validarCierre(nueva, estado, detalles);
        }

        if (!citaDAO.actualizar(nueva)) {
            throw new RecursoNoEncontradoException("Cita", id);
        }
        return buscarPorId(id);
    }

    private void validarCierre(CitaVO cita, EstadoCita estado, List<CitaServicioVO> detalles) {
        boolean yaInicio = !cita.fechaHoraInicio().isAfter(LocalDateTime.now());
        switch (estado) {
            case COMPLETADA -> {
                if (!yaInicio) {
                    throw new ReglaNegocioException("No se puede completar una cita que aún no ha iniciado");
                }
                if (detalles.isEmpty()) {
                    throw new ReglaNegocioException("No se puede completar una cita sin servicios registrados");
                }
            }
            case NO_ASISTIO -> {
                if (!yaInicio) {
                    throw new ReglaNegocioException(
                            "No se puede marcar inasistencia antes de la hora de inicio de la cita");
                }
                validarSinPagosPagados(cita.idCita());
            }
            case CANCELADA -> validarSinPagosPagados(cita.idCita());
            default -> {
                // PROGRAMADA, CONFIRMADA y EN_PROCESO no requieren validaciones de cierre.
            }
        }
    }

    private void validarSinPagosPagados(long idCita) {
        boolean hayPagados = pagoDAO.listarPorCita(idCita).stream()
                .anyMatch(pago -> pago.estado() == EstadoPago.PAGADO);
        if (hayPagados) {
            throw new ReglaNegocioException(
                    "La cita tiene pagos en estado PAGADO; reembólselos o anúlelos antes de cerrarla");
        }
    }

    private static void validarTransicion(EstadoCita actual, EstadoCita nuevo) {
        if (actual == nuevo) {
            if (!ESTADOS_VIGENTES.contains(actual)) {
                throw new ReglaNegocioException("La cita está " + actual + " y ya no admite cambios");
            }
            return;
        }
        if (!TRANSICIONES.get(actual).contains(nuevo)) {
            throw new ReglaNegocioException("No se permite cambiar una cita de " + actual + " a " + nuevo);
        }
    }

    private void validarCliente(long idCliente) {
        ClienteVO cliente = clienteService.buscarPorId(idCliente);
        if (cliente.estado() != EstadoCliente.ACTIVO) {
            throw new ReglaNegocioException("El cliente " + idCliente + " está INACTIVO");
        }
    }

    private void validarBarbero(long idBarbero) {
        BarberoVO barbero = barberoService.buscarPorId(idBarbero);
        if (barbero.estado() != EstadoBarbero.ACTIVO) {
            throw new ReglaNegocioException(
                    "El barbero " + idBarbero + " está " + barbero.estado() + " y no puede recibir citas");
        }
    }

    private static void validarInicioNoPasado(LocalDateTime inicio) {
        if (inicio.isBefore(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))) {
            throw new ReglaNegocioException("La cita no puede iniciar en el pasado");
        }
    }

    private void validarAgenda(CitaVO cita, Long idCitaExcluida) {
        horarioService.validarDentroDeHorario(cita.idBarbero(), cita.fechaHoraInicio(), cita.fechaHoraFin());
        if (citaDAO.existeSolapamiento(
                cita.idBarbero(), cita.fechaHoraInicio(), cita.fechaHoraFin(), idCitaExcluida)) {
            throw new ReglaNegocioException("El barbero " + cita.idBarbero()
                    + " ya tiene una cita que se cruza con el horario solicitado");
        }
    }

    private static <T> T protegerAgenda(Supplier<T> operacion) {
        try {
            return operacion.get();
        } catch (ConcurrencyFailureException e) {
            throw new ReglaNegocioException(
                    "La agenda del barbero fue modificada al mismo tiempo por otra operación; intente nuevamente",
                    e);
        }
    }

    private static LocalDateTime aMinutos(LocalDateTime fecha) {
        return fecha.truncatedTo(ChronoUnit.MINUTES);
    }

    private static CitaVO normalizar(CitaVO c, Long id, EstadoCita estado) {
        Validador.idPositivo(c.idCliente(), "idCliente");
        Validador.idPositivo(c.idBarbero(), "idBarbero");
        LocalDateTime inicio = aMinutos(Validador.requerido(c.fechaHoraInicio(), "fechaHoraInicio"));
        LocalDateTime fin = aMinutos(Validador.requerido(c.fechaHoraFin(), "fechaHoraFin"));
        if (!inicio.isBefore(fin)) {
            throw new ValidacionException("La fecha y hora de inicio debe ser anterior a la de fin");
        }
        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            throw new ValidacionException("La cita debe iniciar y terminar el mismo día");
        }
        return new CitaVO(
                id,
                c.idCliente(),
                c.idBarbero(),
                inicio,
                fin,
                estado,
                Validador.textoOpcional(c.observaciones(), "observaciones", Validador.MAX_TEXTO),
                null,
                null);
    }
}
