package com.uniremignton.barberia.model.vo;

import java.time.LocalDateTime;

/** Value object inmutable que representa una entrada o salida de inventario. */
public record MovimientoInventarioVO(
        Long idMovimiento,
        Long idProducto,
        Integer cantidad,
        LocalDateTime fecha,
        TipoMovimientoInventario tipo) {

    public MovimientoInventarioVO() {
        this(null, null, null, null, null);
    }
}
