package com.uniremignton.barberia.service;

import com.uniremignton.barberia.model.dao.BarberoDAO;
import com.uniremignton.barberia.model.vo.BarberoVO;
import com.uniremignton.barberia.model.vo.EstadoBarbero;
import com.uniremignton.barberia.service.exception.RecursoNoEncontradoException;
import com.uniremignton.barberia.service.exception.ReglaNegocioException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BarberoService {

    private final BarberoDAO barberoDAO;

    public BarberoService(BarberoDAO barberoDAO) {
        this.barberoDAO = barberoDAO;
    }

    /** Registra un barbero; si no trae estado, queda ACTIVO. */
    @Transactional
    public BarberoVO registrar(BarberoVO barbero) {
        Validador.requerido(barbero, "barbero");
        EstadoBarbero estado = barbero.estado() != null ? barbero.estado() : EstadoBarbero.ACTIVO;
        BarberoVO nuevo = normalizar(barbero, null, estado);
        try {
            return barberoDAO.crear(nuevo);
        } catch (DuplicateKeyException e) {
            throw documentoDuplicado(nuevo, e);
        }
    }

    @Transactional
    public BarberoVO actualizar(BarberoVO barbero) {
        Validador.requerido(barbero, "barbero");
        long id = Validador.idPositivo(barbero.idBarbero(), "idBarbero");
        buscarPorId(id);
        BarberoVO actualizado = normalizar(barbero, id, Validador.requerido(barbero.estado(), "estado"));
        try {
            if (!barberoDAO.actualizar(actualizado)) {
                throw new RecursoNoEncontradoException("Barbero", id);
            }
        } catch (DuplicateKeyException e) {
            throw documentoDuplicado(actualizado, e);
        }
        return buscarPorId(id);
    }

    public BarberoVO buscarPorId(long idBarbero) {
        Validador.idPositivo(idBarbero, "idBarbero");
        try {
            return barberoDAO.buscarPorId(idBarbero);
        } catch (EmptyResultDataAccessException e) {
            throw new RecursoNoEncontradoException("Barbero", idBarbero);
        }
    }

    public List<BarberoVO> listar() {
        return barberoDAO.listar();
    }

    @Transactional
    public void eliminar(long idBarbero) {
        buscarPorId(idBarbero);
        try {
            if (!barberoDAO.eliminar(idBarbero)) {
                throw new RecursoNoEncontradoException("Barbero", idBarbero);
            }
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException(
                    "No se puede eliminar el barbero porque tiene servicios, horarios o citas asociados; "
                            + "cambie su estado a INACTIVO",
                    e);
        }
    }

    private static BarberoVO normalizar(BarberoVO b, Long id, EstadoBarbero estado) {
        return new BarberoVO(
                id,
                Validador.textoObligatorio(b.nombres(), "nombres", 100),
                Validador.textoObligatorio(b.apellidos(), "apellidos", 100),
                Validador.textoObligatorio(b.documentoIdentidad(), "documentoIdentidad", 30),
                Validador.telefonoOpcional(b.telefono(), "telefono"),
                Validador.emailOpcional(b.email(), "email", 150),
                b.fechaContratacion(),
                Validador.porcentajeOpcional(b.porcentajeComision(), "porcentajeComision"),
                estado,
                null,
                null);
    }

    private static ReglaNegocioException documentoDuplicado(BarberoVO barbero, DuplicateKeyException causa) {
        return new ReglaNegocioException(
                "Ya existe un barbero con el documento de identidad " + barbero.documentoIdentidad(), causa);
    }
}
