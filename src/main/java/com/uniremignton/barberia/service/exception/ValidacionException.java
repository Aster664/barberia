package com.uniremignton.barberia.service.exception;

/** Se lanza cuando los datos recibidos no cumplen las validaciones básicas. */
public class ValidacionException extends RuntimeException {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
