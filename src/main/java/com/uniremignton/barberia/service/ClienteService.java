package com.uniremignton.barberia.service;

import com.uniremignton.barberia.model.dao.ClienteDAO;
import com.uniremignton.barberia.model.vo.ClienteVO;
import com.uniremignton.barberia.model.vo.EstadoCliente;
import com.uniremignton.barberia.service.exception.RecursoNoEncontradoException;
import com.uniremignton.barberia.service.exception.ReglaNegocioException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteDAO clienteDAO;

    public ClienteService(ClienteDAO clienteDAO) {
        this.clienteDAO = clienteDAO;
    }

    /** Registra un cliente; si no trae estado, queda ACTIVO. */
    @Transactional
    public ClienteVO registrar(ClienteVO cliente) {
        Validador.requerido(cliente, "cliente");
        EstadoCliente estado = cliente.estado() != null ? cliente.estado() : EstadoCliente.ACTIVO;
        return clienteDAO.crear(normalizar(cliente, null, estado));
    }

    @Transactional
    public ClienteVO actualizar(ClienteVO cliente) {
        Validador.requerido(cliente, "cliente");
        long id = Validador.idPositivo(cliente.idCliente(), "idCliente");
        buscarPorId(id);
        ClienteVO actualizado = normalizar(cliente, id, Validador.requerido(cliente.estado(), "estado"));
        if (!clienteDAO.actualizar(actualizado)) {
            throw new RecursoNoEncontradoException("Cliente", id);
        }
        return buscarPorId(id);
    }

    public ClienteVO buscarPorId(long idCliente) {
        Validador.idPositivo(idCliente, "idCliente");
        try {
            return clienteDAO.buscarPorId(idCliente);
        } catch (EmptyResultDataAccessException e) {
            throw new RecursoNoEncontradoException("Cliente", idCliente);
        }
    }

    public List<ClienteVO> listar() {
        return clienteDAO.listar();
    }

    @Transactional
    public void eliminar(long idCliente) {
        buscarPorId(idCliente);
        try {
            if (!clienteDAO.eliminar(idCliente)) {
                throw new RecursoNoEncontradoException("Cliente", idCliente);
            }
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException(
                    "No se puede eliminar el cliente porque tiene citas asociadas; "
                            + "cambie su estado a INACTIVO",
                    e);
        }
    }

    private static ClienteVO normalizar(ClienteVO c, Long id, EstadoCliente estado) {
        return new ClienteVO(
                id,
                Validador.textoObligatorio(c.nombres(), "nombres", 100),
                Validador.textoObligatorio(c.apellidos(), "apellidos", 100),
                Validador.telefonoOpcional(c.telefono(), "telefono"),
                Validador.emailOpcional(c.email(), "email", 150),
                Validador.fechaNoFutura(c.fechaNacimiento(), "fechaNacimiento"),
                Validador.textoOpcional(c.notas(), "notas", Validador.MAX_TEXTO),
                estado,
                null,
                null);
    }
}
