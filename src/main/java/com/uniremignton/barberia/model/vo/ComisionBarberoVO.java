package com.uniremignton.barberia.model.vo;

import java.math.BigDecimal;

/** Proyección de la vista v_comisiones_barberos. */
public record ComisionBarberoVO(
        Long idBarbero,
        String barbero,
        String periodo,
        Long citasCompletadas,
        BigDecimal ingresosGenerados,
        BigDecimal porcentajeComision,
        BigDecimal comision) {

    public ComisionBarberoVO() {
        this(null, null, null, null, null, null, null);
    }
}