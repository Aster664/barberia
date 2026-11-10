package com.uniremignton.barberia.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Value object inmutable que representa un barbero. */
public record BarberoVO(
        Long idBarbero,
        String nombres,
        String apellidos,
        String documentoIdentidad,
        String telefono,
        String email,
        LocalDate fechaContratacion,
        BigDecimal porcentajeComision,
        EstadoBarbero estado,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public BarberoVO() {
        this(null, null, null, null, null, null, null, null, null, null, null);
    }
}