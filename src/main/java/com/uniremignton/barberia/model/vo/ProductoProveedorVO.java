package com.uniremignton.barberia.model.vo;

/** Value object de la relación muchos a muchos entre productos y proveedores. */
public record ProductoProveedorVO(Long idProducto, Long idProveedor) {

    public ProductoProveedorVO() {
        this(null, null);
    }
}
