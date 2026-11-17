package com.uniremignton.barberia.service;

import com.uniremignton.barberia.model.dao.PagoDAO;
import com.uniremignton.barberia.model.vo.CitaVO;
import com.uniremignton.barberia.model.vo.EstadoCita;
import com.uniremignton.barberia.model.vo.EstadoPago;
import com.uniremignton.barberia.model.vo.PagoVO;
import com.uniremignton.barberia.service.exception.RecursoNoEncontradoException;
import com.uniremignton.barberia.service.exception.ReglaNegocioException;
import com.uniremignton.barberia.service.exception.ValidacionException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestiona los pagos y abonos de las citas. */
@Service
@Transactional(readOnly = true)
public class PagoService {

    private static final Map<EstadoPago, Set<EstadoPago>> TRANSICIONES = Map.of(
            EstadoPago.PENDIENTE, EnumSet.of(EstadoPago.PAGADO, EstadoPago.ANULADO),
            EstadoPago.PAGADO, EnumSet.of(EstadoPago.REEMBOLSADO, EstadoPago.ANULADO),
            EstadoPago.REEMBOLSADO, EnumSet.noneOf(EstadoPago.class),
            EstadoPago.ANULADO, EnumSet.noneOf(EstadoPago.class));

    private final PagoDAO pagoDAO;
    private final CitaService citaService;
    private final CitaServicioService citaServicioService;

    public PagoService(PagoDAO pagoDAO, CitaService citaService, CitaServicioService citaServicioService) {
        this.pagoDAO = pagoDAO;
        this.citaService = citaService;
        this.citaServicioService = citaServicioService;
    }

    /**
     * Registra un pago PENDIENTE (por defecto) o PAGADO. El monto no puede superar el saldo de la cita
     * (total de servicios menos lo ya pagado). Un pago PAGADO sin fecha recibe la fecha actual.
     */
    @Transactional
    public PagoVO registrar(PagoVO pago) {
        Validador.requerido(pago, "pago");
        EstadoPago estado = pago.estado() != null ? pago.estado() : EstadoPago.PENDIENTE;
        if (estado != EstadoPago.PENDIENTE && estado != EstadoPago.PAGADO) {
            throw new ReglaNegocioException("Un pago nuevo solo puede registrarse como PENDIENTE o PAGADO");
        }
        PagoVO nuevo = normalizar(pago, null, estado, null);
        validarCitaAdmitePagos(citaService.buscarPorId(nuevo.idCita()));
        validarSaldo(nuevo, null);
        return pagoDAO.crear(nuevo);
    }

    /**
     * Actualiza un pago. Solo un pago PENDIENTE admite cambios de monto, propina o método; un pago
     * PAGADO únicamente admite cambios de estado (REEMBOLSADO o ANULADO) y de referencia. Los pagos
     * REEMBOLSADO y ANULADO no se modifican. Un pago no puede cambiar de cita.
     */
    @Transactional
    public PagoVO actualizar(PagoVO pago) {
        return actualizarPago(pago);
    }

    /** Cambia únicamente el estado respetando las transiciones permitidas. */
    @Transactional
    public PagoVO cambiarEstado(long idPago, EstadoPago nuevoEstado) {
        PagoVO actual = buscarPorId(idPago);
        return actualizarPago(new PagoVO(
                actual.idPago(), actual.idCita(), actual.monto(), actual.propina(), actual.metodoPago(),
                Validador.requerido(nuevoEstado, "estado"), actual.referencia(), actual.fechaPago(), null));
    }

    public PagoVO buscarPorId(long idPago) {
        Validador.idPositivo(idPago, "idPago");
        try {
            return pagoDAO.buscarPorId(idPago);
        } catch (EmptyResultDataAccessException e) {
            throw new RecursoNoEncontradoException("Pago", idPago);
        }
    }

    public List<PagoVO> listar() {
        return pagoDAO.listar();
    }

    public List<PagoVO> listarPorCita(long idCita) {
        citaService.buscarPorId(idCita);
        return pagoDAO.listarPorCita(idCita);
    }

    /** Solo se eliminan pagos PENDIENTE o ANULADO; los pagos PAGADO y REEMBOLSADO se conservan como histórico. */
    @Transactional
    public void eliminar(long idPago) {
        PagoVO pago = buscarPorId(idPago);
        if (pago.estado() != EstadoPago.PENDIENTE && pago.estado() != EstadoPago.ANULADO) {
            throw new ReglaNegocioException("Solo se pueden eliminar pagos PENDIENTE o ANULADO");
        }
        if (!pagoDAO.eliminar(idPago)) {
            throw new RecursoNoEncontradoException("Pago", idPago);
        }
    }

    /** Suma los montos de los pagos PAGADO, ignorando opcionalmente el pago {@code idExcluido}. */
    static BigDecimal sumarPagados(List<PagoVO> pagos, Long idExcluido) {
        return pagos.stream()
                .filter(p -> p.estado() == EstadoPago.PAGADO && !p.idPago().equals(idExcluido))
                .map(PagoVO::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PagoVO actualizarPago(PagoVO pago) {
        Validador.requerido(pago, "pago");
        long id = Validador.idPositivo(pago.idPago(), "idPago");
        PagoVO actual = buscarPorId(id);
        EstadoPago estado = Validador.requerido(pago.estado(), "estado");
        validarTransicion(actual.estado(), estado);
        PagoVO nuevo = normalizar(pago, id, estado, actual.fechaPago());

        if (!nuevo.idCita().equals(actual.idCita())) {
            throw new ReglaNegocioException("Un pago no puede cambiar de cita");
        }
        if (actual.estado() != EstadoPago.PENDIENTE && !mismosDatosFinancieros(actual, nuevo)) {
            throw new ReglaNegocioException("Un pago " + actual.estado()
                    + " solo admite cambios de estado y de referencia");
        }
        if (actual.estado() == EstadoPago.PENDIENTE
                && (estado == EstadoPago.PENDIENTE || estado == EstadoPago.PAGADO)) {
            validarCitaAdmitePagos(citaService.buscarPorId(nuevo.idCita()));
            validarSaldo(nuevo, id);
        }

        if (!pagoDAO.actualizar(nuevo)) {
            throw new RecursoNoEncontradoException("Pago", id);
        }
        return buscarPorId(id);
    }

    private void validarSaldo(PagoVO pago, Long idPagoExcluido) {
        BigDecimal total = citaServicioService.calcularTotal(pago.idCita());
        BigDecimal pagado = sumarPagados(pagoDAO.listarPorCita(pago.idCita()), idPagoExcluido);
        BigDecimal saldo = total.subtract(pagado);
        if (pago.monto().compareTo(saldo) > 0) {
            throw new ReglaNegocioException("El monto (" + pago.monto().toPlainString()
                    + ") supera el saldo pendiente de la cita (" + saldo.toPlainString() + ")");
        }
    }

    private static void validarCitaAdmitePagos(CitaVO cita) {
        if (cita.estado() == EstadoCita.CANCELADA || cita.estado() == EstadoCita.NO_ASISTIO) {
            throw new ReglaNegocioException(
                    "No se pueden registrar pagos para una cita " + cita.estado());
        }
    }

    private static void validarTransicion(EstadoPago actual, EstadoPago nuevo) {
        if (actual == nuevo) {
            if (actual == EstadoPago.REEMBOLSADO || actual == EstadoPago.ANULADO) {
                throw new ReglaNegocioException("El pago está " + actual + " y ya no admite cambios");
            }
            return;
        }
        if (!TRANSICIONES.get(actual).contains(nuevo)) {
            throw new ReglaNegocioException("No se permite cambiar un pago de " + actual + " a " + nuevo);
        }
    }

    private static boolean mismosDatosFinancieros(PagoVO a, PagoVO b) {
        return a.monto().compareTo(b.monto()) == 0
                && a.propina().compareTo(b.propina()) == 0
                && a.metodoPago() == b.metodoPago();
    }

    private static PagoVO normalizar(PagoVO p, Long id, EstadoPago estado, LocalDateTime fechaPorDefecto) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fecha = p.fechaPago() != null ? p.fechaPago() : fechaPorDefecto;
        if (fecha != null && fecha.isAfter(ahora)) {
            throw new ValidacionException("La fecha de pago no puede ser futura");
        }
        if (estado == EstadoPago.PENDIENTE) {
            fecha = null;
        } else if (estado == EstadoPago.PAGADO && fecha == null) {
            fecha = ahora.truncatedTo(ChronoUnit.SECONDS);
        }
        BigDecimal propina = p.propina() == null
                ? BigDecimal.ZERO.setScale(2)
                : Validador.monto(p.propina(), "propina");
        return new PagoVO(
                id,
                Validador.idPositivo(p.idCita(), "idCita"),
                Validador.montoPositivo(p.monto(), "monto"),
                propina,
                Validador.requerido(p.metodoPago(), "metodoPago"),
                estado,
                Validador.textoOpcional(p.referencia(), "referencia", 255),
                fecha,
                null);
    }
}
