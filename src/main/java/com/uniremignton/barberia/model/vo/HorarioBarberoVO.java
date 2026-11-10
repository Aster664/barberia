package com.uniremignton.barberia.model.vo;

import java.time.LocalTime;

/** Value object para un bloque del horario semanal del barbero. */
public record HorarioBarberoVO(
        Long idHorario,
        Long idBarbero,
        Integer diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin) {

    public HorarioBarberoVO() {
        this(null, null, null, null, null);
    }
}