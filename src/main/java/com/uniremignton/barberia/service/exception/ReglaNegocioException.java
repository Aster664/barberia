package com.uniremignton.barberia.service.exception;

/** Se lanza cuando una operación viola una regla de negocio o una restricción de integridad. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }

    public ReglaNegocioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
