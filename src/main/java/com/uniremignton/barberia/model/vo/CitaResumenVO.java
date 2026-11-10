package com.uniremignton.barberia.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Proyección de la vista v_citas_resumen. */
public record CitaResumenVO(
        Long idCita,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        EstadoCita estado,
        String cliente,
        String barbero,
        BigDecimal totalServicios,
        BigDecimal totalPagado,
        BigDecimal saldoPendiente) {

    public CitaResumenVO() {
        this(null, null, null, null, null, null, null, null, null);
    }
}