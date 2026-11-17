package com.uniremignton.barberia.service;

import com.uniremignton.barberia.model.dao.ServicioDAO;
import com.uniremignton.barberia.model.vo.EstadoServicio;
import com.uniremignton.barberia.model.vo.ServicioVO;
import com.uniremignton.barberia.service.exception.RecursoNoEncontradoException;
import com.uniremignton.barberia.service.exception.ReglaNegocioException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ServicioService {

    private final ServicioDAO servicioDAO;

    public ServicioService(ServicioDAO servicioDAO) {
        this.servicioDAO = servicioDAO;
    }

    /** Registra un servicio; si no trae estado, queda ACTIVO. */
    @Transactional
    public ServicioVO registrar(ServicioVO servicio) {
        Validador.requerido(servicio, "servicio");
        EstadoServicio estado = servicio.estado() != null ? servicio.estado() : EstadoServicio.ACTIVO;
        return servicioDAO.crear(normalizar(servicio, null, estado));
    }

    @Transactional
    public ServicioVO actualizar(ServicioVO servicio) {
        Validador.requerido(servicio, "servicio");
        long id = Validador.idPositivo(servicio.idServicio(), "idServicio");
        buscarPorId(id);
        ServicioVO actualizado = normalizar(servicio, id, Validador.requerido(servicio.estado(), "estado"));
        if (!servicioDAO.actualizar(actualizado)) {
            throw new RecursoNoEncontradoException("Servicio", id);
        }
        return buscarPorId(id);
    }

    public ServicioVO buscarPorId(long idServicio) {
        Validador.idPositivo(idServicio, "idServicio");
        try {
            return servicioDAO.buscarPorId(idServicio);
        } catch (EmptyResultDataAccessException e) {
            throw new RecursoNoEncontradoException("Servicio", idServicio);
        }
    }

    public List<ServicioVO> listar() {
        return servicioDAO.listar();
    }

    @Transactional
    public void eliminar(long idServicio) {
        buscarPorId(idServicio);
        try {
            if (!servicioDAO.eliminar(idServicio)) {
                throw new RecursoNoEncontradoException("Servicio", idServicio);
            }
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException(
                    "No se puede eliminar el servicio porque tiene barberos o citas asociados; "
                            + "cambie su estado a INACTIVO",
                    e);
        }
    }

    private static ServicioVO normalizar(ServicioVO s, Long id, EstadoServicio estado) {
        return new ServicioVO(
                id,
                Validador.textoObligatorio(s.nombre(), "nombre", 120),
                Validador.textoOpcional(s.descripcion(), "descripcion", Validador.MAX_TEXTO),
                Validador.enteroPositivo(s.duracionMinutos(), "duracionMinutos"),
                Validador.monto(s.precio(), "precio"),
                estado,
                null,
                null);
    }
}
