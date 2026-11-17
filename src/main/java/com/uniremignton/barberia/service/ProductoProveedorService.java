package com.uniremignton.barberia.service;

import com.uniremignton.barberia.model.dao.ProductoProveedorDAO;
import com.uniremignton.barberia.model.vo.ProductoProveedorVO;
import com.uniremignton.barberia.service.exception.RecursoNoEncontradoException;
import com.uniremignton.barberia.service.exception.ReglaNegocioException;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestiona la relación muchos a muchos entre productos y proveedores. */
@Service
@Transactional(readOnly = true)
public class ProductoProveedorService {

    private final ProductoProveedorDAO productoProveedorDAO;
    private final ProductoService productoService;
    private final ProveedorService proveedorService;

    public ProductoProveedorService(
            ProductoProveedorDAO productoProveedorDAO,
            ProductoService productoService,
            ProveedorService proveedorService) {
        this.productoProveedorDAO = productoProveedorDAO;
        this.productoService = productoService;
        this.proveedorService = proveedorService;
    }

    @Transactional
    public ProductoProveedorVO asociar(ProductoProveedorVO relacion) {
        Validador.requerido(relacion, "relacion");
        long idProducto = Validador.idPositivo(relacion.idProducto(), "idProducto");
        long idProveedor = Validador.idPositivo(relacion.idProveedor(), "idProveedor");

        productoService.buscarPorId(idProducto);
        proveedorService.buscarPorId(idProveedor);

        ProductoProveedorVO nueva = new ProductoProveedorVO(idProducto, idProveedor);
        try {
            productoProveedorDAO.asociar(nueva);
        } catch (DuplicateKeyException e) {
            throw new ReglaNegocioException(
                    "El proveedor " + idProveedor + " ya está asociado al producto " + idProducto, e);
        }
        return nueva;
    }

    @Transactional
    public void desasociar(long idProducto, long idProveedor) {
        Validador.idPositivo(idProducto, "idProducto");
        Validador.idPositivo(idProveedor, "idProveedor");
        if (!productoProveedorDAO.desasociar(idProducto, idProveedor)) {
            throw new RecursoNoEncontradoException(
                    "La relación entre el producto " + idProducto + " y el proveedor " + idProveedor + " no existe");
        }
    }

    public List<ProductoProveedorVO> listarPorProducto(long idProducto) {
        productoService.buscarPorId(idProducto);
        return productoProveedorDAO.listarPorProducto(idProducto);
    }

    public List<ProductoProveedorVO> listarPorProveedor(long idProveedor) {
        proveedorService.buscarPorId(idProveedor);
        return productoProveedorDAO.listarPorProveedor(idProveedor);
    }
}
