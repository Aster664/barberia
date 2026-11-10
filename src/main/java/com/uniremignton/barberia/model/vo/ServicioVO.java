package com.uniremignton.barberia.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Value object inmutable que representa un servicio ofrecido. */
public record ServicioVO(
        Long idServicio,
        String nombre,
        String descripcion,
        Integer duracionMinutos,
        BigDecimal precio,
        EstadoServicio estado,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public ServicioVO() {
        this(null, null, null, null, null, null, null, null);
    }
}