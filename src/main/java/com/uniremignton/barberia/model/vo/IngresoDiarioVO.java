package com.uniremignton.barberia.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Proyección de la vista v_ingresos_diarios. */
public record IngresoDiarioVO(
        LocalDate fecha,
        MetodoPago metodoPago,
        Long numPagos,
        BigDecimal totalMonto,
        BigDecimal totalPropinas) {

    public IngresoDiarioVO() {
        this(null, null, null, null, null);
    }
}