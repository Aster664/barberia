package com.uniremignton.barberia.model.vo;

import java.math.BigDecimal;

/** Value object inmutable que representa un producto del inventario. */
public record ProductoVO(
        Long idProducto,
        String nombre,
        String descripcion,
        BigDecimal precioCompra,
        BigDecimal precioVenta,
        Integer cantidadDisponible,
        Integer stockMinimo) {

    public ProductoVO() {
        this(null, null, null, null, null, null, null);
    }
}
