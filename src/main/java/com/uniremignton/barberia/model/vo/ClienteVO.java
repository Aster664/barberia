package com.uniremignton.barberia.model.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Value object inmutable que representa un cliente. */
public record ClienteVO(
        Long idCliente,
        String nombres,
        String apellidos,
        String telefono,
        String email,
        LocalDate fechaNacimiento,
        String notas,
        EstadoCliente estado,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    /** Constructor vacío requerido por algunos mecanismos de binding. */
    public ClienteVO() {
        this(null, null, null, null, null, null, null, null, null, null);
    }
}