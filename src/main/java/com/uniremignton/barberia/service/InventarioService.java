package com.uniremignton.barberia.service;

import com.uniremignton.barberia.model.dao.MovimientoInventarioDAO;
import com.uniremignton.barberia.model.vo.MovimientoInventarioVO;
import com.uniremignton.barberia.model.vo.ProductoVO;
import com.uniremignton.barberia.model.vo.TipoMovimientoInventario;
import com.uniremignton.barberia.service.exception.RecursoNoEncontradoException;
import com.uniremignton.barberia.service.exception.ReglaNegocioException;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestiona los movimientos de inventario (ENTRADA/SALIDA) y, con ellos, el stock de los productos. */
@Service
@Transactional(readOnly = true)
public class InventarioService {

    private final MovimientoInventarioDAO movimientoDAO;
    private final ProductoService productoService;

    public InventarioService(MovimientoInventarioDAO movimientoDAO, ProductoService productoService) {
        this.movimientoDAO = movimientoDAO;
        this.productoService = productoService;
    }

    /** Registra una entrada o salida y actualiza el stock. Una salida no puede superar el stock disponible. */
    @Transactional
    public MovimientoInventarioVO registrarMovimiento(MovimientoInventarioVO movimiento) {
        Validador.requerido(movimiento, "movimiento");
        long idProducto = Validador.idPositivo(movimiento.idProducto(), "idProducto");
        int cantidad = Validador.enteroPositivo(movimiento.cantidad(), "cantidad");
        TipoMovimientoInventario tipo = Validador.requerido(movimiento.tipo(), "tipo");

        ProductoVO producto = productoService.buscarPorId(idProducto);
        boolean salida = tipo == TipoMovimientoInventario.SALIDA;
        if (salida && producto.cantidadDisponible() < cantidad) {
            throw new ReglaNegocioException("Stock insuficiente para el producto " + producto.nombre()
                    + ": disponible " + producto.cantidadDisponible() + ", solicitado " + cantidad);
        }

        MovimientoInventarioVO nuevo = new MovimientoInventarioVO(
                null, idProducto, cantidad, movimiento.fecha(), tipo);
        try {
            return movimientoDAO.registrar(nuevo);
        } catch (IllegalStateException e) {
            // El DAO protege el stock de forma atómica ante salidas concurrentes.
            if (salida) {
                throw new ReglaNegocioException(
                        "Stock insuficiente para el producto " + producto.nombre(), e);
            }
            throw e;
        }
    }

    public MovimientoInventarioVO buscarPorId(long idMovimiento) {
        Validador.idPositivo(idMovimiento, "idMovimiento");
        try {
            return movimientoDAO.buscarPorId(idMovimiento);
        } catch (EmptyResultDataAccessException e) {
            throw new RecursoNoEncontradoException("Movimiento de inventario", idMovimiento);
        }
    }

    public List<MovimientoInventarioVO> listar() {
        return movimientoDAO.listar();
    }

    public List<MovimientoInventarioVO> listarPorProducto(long idProducto) {
        productoService.buscarPorId(idProducto);
        return movimientoDAO.listarPorProducto(idProducto);
    }
}
