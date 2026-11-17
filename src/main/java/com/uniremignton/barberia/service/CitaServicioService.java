package com.uniremignton.barberia.service;

import com.uniremignton.barberia.model.dao.CitaServicioDAO;
import com.uniremignton.barberia.model.dao.PagoDAO;
import com.uniremignton.barberia.model.vo.CitaServicioVO;
import com.uniremignton.barberia.model.vo.CitaVO;
import com.uniremignton.barberia.model.vo.EstadoServicio;
import com.uniremignton.barberia.model.vo.ServicioVO;
import com.uniremignton.barberia.service.exception.RecursoNoEncontradoException;
import com.uniremignton.barberia.service.exception.ReglaNegocioException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestiona los servicios incluidos en cada cita (tabla cita_servicio). */
@Service
@Transactional(readOnly = true)
public class CitaServicioService {

    private final CitaServicioDAO citaServicioDAO;
    private final PagoDAO pagoDAO;
    private final CitaService citaService;
    private final ServicioService servicioService;

    public CitaServicioService(
            CitaServicioDAO citaServicioDAO,
            PagoDAO pagoDAO,
            CitaService citaService,
            ServicioService servicioService) {
        this.citaServicioDAO = citaServicioDAO;
        this.pagoDAO = pagoDAO;
        this.citaService = citaService;
        this.servicioService = servicioService;
    }

    /**
     * Agrega un servicio ACTIVO a una cita vigente. Si no se indica precio aplicado se usa el precio
     * actual del servicio. El barbero debe ofrecer el servicio y este debe caber en la duración de la cita.
     */
    @Transactional
    public CitaServicioVO asociar(CitaServicioVO detalle) {
        Validador.requerido(detalle, "detalle");
        long idCita = Validador.idPositivo(detalle.idCita(), "idCita");
        long idServicio = Validador.idPositivo(detalle.idServicio(), "idServicio");
        int cantidad = Validador.enteroPositivo(detalle.cantidad(), "cantidad");

        CitaVO cita = citaService.buscarVigente(idCita);
        ServicioVO servicio = servicioService.buscarPorId(idServicio);
        if (servicio.estado() != EstadoServicio.ACTIVO) {
            throw new ReglaNegocioException("No se puede agregar un servicio INACTIVO a una cita");
        }
        BigDecimal precio = detalle.precioAplicado() == null
                ? servicio.precio()
                : Validador.monto(detalle.precioAplicado(), "precioAplicado");

        List<CitaServicioVO> existentes = citaServicioDAO.listarPorCita(idCita);
        if (existentes.stream().anyMatch(d -> d.idServicio().equals(idServicio))) {
            throw servicioYaIncluido(idCita, idServicio, null);
        }
        CitaServicioVO nuevo = new CitaServicioVO(idCita, idServicio, cantidad, precio);
        List<CitaServicioVO> resultante = new ArrayList<>(existentes);
        resultante.add(nuevo);
        citaService.validarServiciosEnCita(cita, resultante);

        try {
            citaServicioDAO.asociar(nuevo);
        } catch (DuplicateKeyException e) {
            throw servicioYaIncluido(idCita, idServicio, e);
        }
        return buscarPorId(idCita, idServicio);
    }

    /** Modifica cantidad y/o precio aplicado; si no se indica precio se conserva el actual. */
    @Transactional
    public CitaServicioVO actualizar(CitaServicioVO detalle) {
        Validador.requerido(detalle, "detalle");
        long idCita = Validador.idPositivo(detalle.idCita(), "idCita");
        long idServicio = Validador.idPositivo(detalle.idServicio(), "idServicio");
        int cantidad = Validador.enteroPositivo(detalle.cantidad(), "cantidad");

        CitaVO cita = citaService.buscarVigente(idCita);
        CitaServicioVO actual = buscarPorId(idCita, idServicio);
        BigDecimal precio = detalle.precioAplicado() == null
                ? actual.precioAplicado()
                : Validador.monto(detalle.precioAplicado(), "precioAplicado");
        CitaServicioVO nuevo = new CitaServicioVO(idCita, idServicio, cantidad, precio);

        List<CitaServicioVO> resultante = new ArrayList<>();
        for (CitaServicioVO existente : citaServicioDAO.listarPorCita(idCita)) {
            resultante.add(existente.idServicio().equals(idServicio) ? nuevo : existente);
        }
        citaService.validarServiciosEnCita(cita, resultante);
        validarTotalCubreLoPagado(idCita, resultante);

        if (!citaServicioDAO.actualizar(nuevo)) {
            throw noIncluido(idCita, idServicio);
        }
        return buscarPorId(idCita, idServicio);
    }

    @Transactional
    public void desasociar(long idCita, long idServicio) {
        Validador.idPositivo(idServicio, "idServicio");
        citaService.buscarVigente(idCita);
        List<CitaServicioVO> resultante = new ArrayList<>();
        for (CitaServicioVO existente : citaServicioDAO.listarPorCita(idCita)) {
            if (!existente.idServicio().equals(idServicio)) {
                resultante.add(existente);
            }
        }
        validarTotalCubreLoPagado(idCita, resultante);
        if (!citaServicioDAO.desasociar(idCita, idServicio)) {
            throw noIncluido(idCita, idServicio);
        }
    }

    public CitaServicioVO buscarPorId(long idCita, long idServicio) {
        Validador.idPositivo(idCita, "idCita");
        Validador.idPositivo(idServicio, "idServicio");
        try {
            return citaServicioDAO.buscarPorId(idCita, idServicio);
        } catch (EmptyResultDataAccessException e) {
            throw noIncluido(idCita, idServicio);
        }
    }

    public List<CitaServicioVO> listarPorCita(long idCita) {
        citaService.buscarPorId(idCita);
        return citaServicioDAO.listarPorCita(idCita);
    }

    public List<CitaServicioVO> listarPorServicio(long idServicio) {
        servicioService.buscarPorId(idServicio);
        return citaServicioDAO.listarPorServicio(idServicio);
    }

    /** Total de la cita: suma de cantidad x precio aplicado de sus servicios. */
    public BigDecimal calcularTotal(long idCita) {
        citaService.buscarPorId(idCita);
        return total(citaServicioDAO.listarPorCita(idCita));
    }

    private void validarTotalCubreLoPagado(long idCita, List<CitaServicioVO> resultante) {
        BigDecimal pagado = PagoService.sumarPagados(pagoDAO.listarPorCita(idCita), null);
        if (total(resultante).compareTo(pagado) < 0) {
            throw new ReglaNegocioException("El cambio dejaría el total de la cita por debajo de lo ya pagado ("
                    + pagado.toPlainString() + ")");
        }
    }

    private static BigDecimal total(List<CitaServicioVO> detalles) {
        return detalles.stream()
                .map(d -> d.precioAplicado().multiply(BigDecimal.valueOf(d.cantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static ReglaNegocioException servicioYaIncluido(long idCita, long idServicio, Throwable causa) {
        String mensaje = "El servicio " + idServicio + " ya está incluido en la cita " + idCita
                + "; actualice su cantidad o precio";
        return causa == null ? new ReglaNegocioException(mensaje) : new ReglaNegocioException(mensaje, causa);
    }

    private static RecursoNoEncontradoException noIncluido(long idCita, long idServicio) {
        return new RecursoNoEncontradoException(
                "El servicio " + idServicio + " no está incluido en la cita " + idCita);
    }
}
