package com.uniremignton.barberia.service;

import com.uniremignton.barberia.model.dao.BarberoServicioDAO;
import com.uniremignton.barberia.model.vo.BarberoServicioVO;
import com.uniremignton.barberia.model.vo.BarberoVO;
import com.uniremignton.barberia.model.vo.EstadoBarbero;
import com.uniremignton.barberia.model.vo.EstadoServicio;
import com.uniremignton.barberia.model.vo.ServicioVO;
import com.uniremignton.barberia.service.exception.RecursoNoEncontradoException;
import com.uniremignton.barberia.service.exception.ReglaNegocioException;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestiona la relación muchos a muchos entre barberos y servicios. */
@Service
@Transactional(readOnly = true)
public class BarberoServicioService {

    private final BarberoServicioDAO barberoServicioDAO;
    private final BarberoService barberoService;
    private final ServicioService servicioService;

    public BarberoServicioService(
            BarberoServicioDAO barberoServicioDAO,
            BarberoService barberoService,
            ServicioService servicioService) {
        this.barberoServicioDAO = barberoServicioDAO;
        this.barberoService = barberoService;
        this.servicioService = servicioService;
    }

    /** Asocia un servicio ACTIVO a un barbero que no esté INACTIVO. */
    @Transactional
    public BarberoServicioVO asociar(BarberoServicioVO relacion) {
        Validador.requerido(relacion, "relacion");
        long idBarbero = Validador.idPositivo(relacion.idBarbero(), "idBarbero");
        long idServicio = Validador.idPositivo(relacion.idServicio(), "idServicio");

        BarberoVO barbero = barberoService.buscarPorId(idBarbero);
        if (barbero.estado() == EstadoBarbero.INACTIVO) {
            throw new ReglaNegocioException("No se pueden asignar servicios a un barbero INACTIVO");
        }
        ServicioVO servicio = servicioService.buscarPorId(idServicio);
        if (servicio.estado() != EstadoServicio.ACTIVO) {
            throw new ReglaNegocioException("No se puede asignar un servicio INACTIVO a un barbero");
        }

        BarberoServicioVO nueva = new BarberoServicioVO(idBarbero, idServicio);
        try {
            barberoServicioDAO.asociar(nueva);
        } catch (DuplicateKeyException e) {
            throw new ReglaNegocioException(
                    "El servicio " + idServicio + " ya está asociado al barbero " + idBarbero, e);
        }
        return nueva;
    }

    @Transactional
    public void desasociar(long idBarbero, long idServicio) {
        Validador.idPositivo(idBarbero, "idBarbero");
        Validador.idPositivo(idServicio, "idServicio");
        if (!barberoServicioDAO.desasociar(idBarbero, idServicio)) {
            throw new RecursoNoEncontradoException(
                    "La relación entre el barbero " + idBarbero + " y el servicio " + idServicio + " no existe");
        }
    }

    public List<BarberoServicioVO> listarPorBarbero(long idBarbero) {
        barberoService.buscarPorId(idBarbero);
        return barberoServicioDAO.listarPorBarbero(idBarbero);
    }

    public List<BarberoServicioVO> listarPorServicio(long idServicio) {
        servicioService.buscarPorId(idServicio);
        return barberoServicioDAO.listarPorServicio(idServicio);
    }
}
