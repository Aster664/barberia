package com.uniremignton.barberia.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Value object inmutable que representa un pago o abono de una cita. */
public record PagoVO(
        Long idPago,
        Long idCita,
        BigDecimal monto,
        BigDecimal propina,
        MetodoPago metodoPago,
        EstadoPago estado,
        String referencia,
        LocalDateTime fechaPago,
        LocalDateTime createdAt) {

    public PagoVO() {
        this(null, null, null, null, null, null, null, null, null);
    }
}