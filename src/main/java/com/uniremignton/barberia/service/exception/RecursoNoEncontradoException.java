package com.uniremignton.barberia.service.exception;

/** Se lanza cuando un registro solicitado no existe en la base de datos. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public RecursoNoEncontradoException(String entidad, long id) {
        super(entidad + " con id " + id + " no existe");
    }
}
