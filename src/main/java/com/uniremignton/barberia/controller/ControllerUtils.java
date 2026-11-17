package com.uniremignton.barberia.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Utilidades internas compartidas por los controladores. */
final class ControllerUtils {

    private ControllerUtils() {
    }

    /**
     * Indexa una lista por identificador conservando el orden, para que las vistas puedan resolver
     * nombres a partir de un id (por ejemplo {@code clientesPorId[cita.idCliente]}).
     */
    static <T> Map<Long, T> indexar(List<T> elementos, Function<T, Long> id) {
        Map<Long, T> mapa = new LinkedHashMap<>();
        for (T elemento : elementos) {
            mapa.put(id.apply(elemento), elemento);
        }
        return mapa;
    }
}
