package com.uniremignton.barberia.service;

import com.uniremignton.barberia.service.exception.ValidacionException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.regex.Pattern;

/** Validaciones básicas compartidas por los Service. Alineadas con los tipos de schema.sql. */
final class Validador {

    static final int MAX_TEXTO = 65_535;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern TELEFONO = Pattern.compile("^[0-9+()\\-.\\s]+$");
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private Validador() {
    }

    static <T> T requerido(T valor, String campo) {
        if (valor == null) {
            throw new ValidacionException("El campo " + campo + " es obligatorio");
        }
        return valor;
    }

    static long idPositivo(Long id, String campo) {
        if (requerido(id, campo) <= 0) {
            throw new ValidacionException("El campo " + campo + " debe ser mayor que cero");
        }
        return id;
    }

    /** Devuelve el texto sin espacios laterales, o null si es nulo o está en blanco. */
    static String textoOpcional(String valor, String campo, int maximo) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.isEmpty()) {
            return null;
        }
        if (limpio.length() > maximo) {
            throw new ValidacionException(
                    "El campo " + campo + " no puede superar los " + maximo + " caracteres");
        }
        return limpio;
    }

    static String textoObligatorio(String valor, String campo, int maximo) {
        String limpio = textoOpcional(valor, campo, maximo);
        if (limpio == null) {
            throw new ValidacionException("El campo " + campo + " es obligatorio");
        }
        return limpio;
    }

    static String telefonoOpcional(String valor, String campo) {
        String limpio = textoOpcional(valor, campo, 30);
        if (limpio != null && !TELEFONO.matcher(limpio).matches()) {
            throw new ValidacionException("El campo " + campo + " tiene un formato inválido");
        }
        return limpio;
    }

    static String emailOpcional(String valor, String campo, int maximo) {
        String limpio = textoOpcional(valor, campo, maximo);
        if (limpio != null && !EMAIL.matcher(limpio).matches()) {
            throw new ValidacionException("El campo " + campo + " tiene un formato inválido");
        }
        return limpio;
    }

    /** Monto obligatorio, no negativo, compatible con DECIMAL(12,2). */
    static BigDecimal monto(BigDecimal valor, String campo) {
        BigDecimal ajustado = conDosDecimales(requerido(valor, campo), campo);
        if (ajustado.signum() < 0) {
            throw new ValidacionException("El campo " + campo + " no puede ser negativo");
        }
        if (ajustado.precision() > 12) {
            throw new ValidacionException("El campo " + campo + " excede el valor máximo permitido");
        }
        return ajustado;
    }

    /** Porcentaje opcional entre 0 y 100, compatible con DECIMAL(5,2). */
    static BigDecimal porcentajeOpcional(BigDecimal valor, String campo) {
        if (valor == null) {
            return null;
        }
        BigDecimal ajustado = conDosDecimales(valor, campo);
        if (ajustado.signum() < 0 || ajustado.compareTo(CIEN) > 0) {
            throw new ValidacionException("El campo " + campo + " debe estar entre 0 y 100");
        }
        return ajustado;
    }

    /** Entero no negativo; un valor nulo se interpreta como cero. */
    static int enteroNoNegativo(Integer valor, String campo) {
        if (valor == null) {
            return 0;
        }
        if (valor < 0) {
            throw new ValidacionException("El campo " + campo + " no puede ser negativo");
        }
        return valor;
    }

    static int enteroPositivo(Integer valor, String campo) {
        if (requerido(valor, campo) <= 0) {
            throw new ValidacionException("El campo " + campo + " debe ser mayor que cero");
        }
        return valor;
    }

    /** Monto obligatorio y estrictamente mayor que cero. */
    static BigDecimal montoPositivo(BigDecimal valor, String campo) {
        BigDecimal ajustado = monto(valor, campo);
        if (ajustado.signum() == 0) {
            throw new ValidacionException("El campo " + campo + " debe ser mayor que cero");
        }
        return ajustado;
    }

    /** Fecha opcional que no puede estar en el futuro. */
    static LocalDate fechaNoFutura(LocalDate fecha, String campo) {
        if (fecha != null && fecha.isAfter(LocalDate.now())) {
            throw new ValidacionException("El campo " + campo + " no puede ser una fecha futura");
        }
        return fecha;
    }

    static int enteroEnRango(Integer valor, String campo, int minimo, int maximo) {
        int numero = requerido(valor, campo);
        if (numero < minimo || numero > maximo) {
            throw new ValidacionException(
                    "El campo " + campo + " debe estar entre " + minimo + " y " + maximo);
        }
        return numero;
    }

    private static BigDecimal conDosDecimales(BigDecimal valor, String campo) {
        try {
            return valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new ValidacionException("El campo " + campo + " no puede tener más de 2 decimales");
        }
    }
}
