package com.uniremignton.barberia.service;

import com.uniremignton.barberia.model.dao.ProductoDAO;
import com.uniremignton.barberia.model.vo.ProductoVO;
import com.uniremignton.barberia.service.exception.RecursoNoEncontradoException;
import com.uniremignton.barberia.service.exception.ReglaNegocioException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoDAO productoDAO;

    public ProductoService(ProductoDAO productoDAO) {
        this.productoDAO = productoDAO;
    }

    /** Registra un producto. La cantidad inicial (nula = 0) la convierte el DAO en un movimiento de ENTRADA. */
    @Transactional
    public ProductoVO registrar(ProductoVO producto) {
        Validador.requerido(producto, "producto");
        int stockInicial = Validador.enteroNoNegativo(producto.cantidadDisponible(), "cantidadDisponible");
        return productoDAO.crear(normalizar(producto, null, stockInicial));
    }

    /**
     * Actualiza los datos del producto. La cantidad disponible no se modifica aquí
     * (el DAO no la actualiza); se gestiona con {@link InventarioService}.
     */
    @Transactional
    public ProductoVO actualizar(ProductoVO producto) {
        Validador.requerido(producto, "producto");
        long id = Validador.idPositivo(producto.idProducto(), "idProducto");
        ProductoVO actual = buscarPorId(id);
        ProductoVO actualizado = normalizar(producto, id, actual.cantidadDisponible());
        if (!productoDAO.actualizar(actualizado)) {
            throw new RecursoNoEncontradoException("Producto", id);
        }
        return buscarPorId(id);
    }

    public ProductoVO buscarPorId(long idProducto) {
        Validador.idPositivo(idProducto, "idProducto");
        try {
            return productoDAO.buscarPorId(idProducto);
        } catch (EmptyResultDataAccessException e) {
            throw new RecursoNoEncontradoException("Producto", idProducto);
        }
    }

    public List<ProductoVO> listar() {
        return productoDAO.listar();
    }

    @Transactional
    public void eliminar(long idProducto) {
        buscarPorId(idProducto);
        try {
            if (!productoDAO.eliminar(idProducto)) {
                throw new RecursoNoEncontradoException("Producto", idProducto);
            }
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException(
                    "No se puede eliminar el producto porque tiene movimientos de inventario o proveedores asociados",
                    e);
        }
    }

    private static ProductoVO normalizar(ProductoVO p, Long id, int cantidadDisponible) {
        return new ProductoVO(
                id,
                Validador.textoObligatorio(p.nombre(), "nombre", 120),
                Validador.textoOpcional(p.descripcion(), "descripcion", Validador.MAX_TEXTO),
                Validador.monto(p.precioCompra(), "precioCompra"),
                Validador.monto(p.precioVenta(), "precioVenta"),
                cantidadDisponible,
                Validador.enteroNoNegativo(p.stockMinimo(), "stockMinimo"));
    }
}
