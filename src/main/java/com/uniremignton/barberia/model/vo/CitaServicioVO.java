package com.uniremignton.barberia.model.vo;

import java.math.BigDecimal;

/** Value object del detalle de servicios incluidos en una cita. */
public record CitaServicioVO(
        Long idCita,
        Long idServicio,
        Integer cantidad,
        BigDecimal precioAplicado) {

    public CitaServicioVO() {
        this(null, null, null, null);
    }
}