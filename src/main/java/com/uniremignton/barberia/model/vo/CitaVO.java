package com.uniremignton.barberia.model.vo;

import java.time.LocalDateTime;

/** Value object inmutable que representa una cita. */
public record CitaVO(
        Long idCita,
        Long idCliente,
        Long idBarbero,
        LocalDateTime fechaHoraInicio,
        LocalDateTime fechaHoraFin,
        EstadoCita estado,
        String observaciones,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public CitaVO() {
        this(null, null, null, null, null, null, null, null, null);
    }
}