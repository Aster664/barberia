package com.uniremignton.barberia.service;

import com.uniremignton.barberia.model.dao.ProveedorDAO;
import com.uniremignton.barberia.model.vo.ProveedorVO;
import com.uniremignton.barberia.service.exception.RecursoNoEncontradoException;
import com.uniremignton.barberia.service.exception.ReglaNegocioException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProveedorService {

    private final ProveedorDAO proveedorDAO;

    public ProveedorService(ProveedorDAO proveedorDAO) {
        this.proveedorDAO = proveedorDAO;
    }

    @Transactional
    public ProveedorVO registrar(ProveedorVO proveedor) {
        Validador.requerido(proveedor, "proveedor");
        return proveedorDAO.crear(normalizar(proveedor, null));
    }

    @Transactional
    public ProveedorVO actualizar(ProveedorVO proveedor) {
        Validador.requerido(proveedor, "proveedor");
        long id = Validador.idPositivo(proveedor.idProveedor(), "idProveedor");
        buscarPorId(id);
        if (!proveedorDAO.actualizar(normalizar(proveedor, id))) {
            throw new RecursoNoEncontradoException("Proveedor", id);
        }
        return buscarPorId(id);
    }

    public ProveedorVO buscarPorId(long idProveedor) {
        Validador.idPositivo(idProveedor, "idProveedor");
        try {
            return proveedorDAO.buscarPorId(idProveedor);
        } catch (EmptyResultDataAccessException e) {
            throw new RecursoNoEncontradoException("Proveedor", idProveedor);
        }
    }

    public List<ProveedorVO> listar() {
        return proveedorDAO.listar();
    }

    @Transactional
    public void eliminar(long idProveedor) {
        buscarPorId(idProveedor);
        try {
            if (!proveedorDAO.eliminar(idProveedor)) {
                throw new RecursoNoEncontradoException("Proveedor", idProveedor);
            }
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException(
                    "No se puede eliminar el proveedor porque tiene productos asociados", e);
        }
    }

    private static ProveedorVO normalizar(ProveedorVO p, Long id) {
        return new ProveedorVO(
                id,
                Validador.textoObligatorio(p.nombre(), "nombre", 120),
                Validador.telefonoOpcional(p.telefono(), "telefono"),
                Validador.emailOpcional(p.correo(), "correo", 150),
                Validador.textoOpcional(p.direccion(), "direccion", 255));
    }
}
