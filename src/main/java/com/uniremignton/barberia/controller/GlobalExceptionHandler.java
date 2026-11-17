package com.uniremignton.barberia.controller;

import com.uniremignton.barberia.service.exception.RecursoNoEncontradoException;
import com.uniremignton.barberia.service.exception.ReglaNegocioException;
import com.uniremignton.barberia.service.exception.ValidacionException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Manejo centralizado de errores de los controladores. Convierte las excepciones de los Service en
 * un mensaje flash ({@code error}) y redirige a la página desde la que se hizo la petición
 * (o, si no se conoce, a la sección del módulo). Los éxitos usan el atributo flash {@code mensaje}.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ValidacionException.class, ReglaNegocioException.class, RecursoNoEncontradoException.class})
    public String manejarErroresDeNegocio(RuntimeException e, HttpServletRequest request, RedirectAttributes ra) {
        return volver(request, ra, e.getMessage());
    }

    @ExceptionHandler(BindException.class)
    public String manejarDatosInvalidos(BindException e, HttpServletRequest request, RedirectAttributes ra) {
        String campos = e.getFieldErrors().stream()
                .map(FieldError::getField)
                .distinct()
                .collect(Collectors.joining(", "));
        String mensaje = "Los datos enviados no son válidos" + (campos.isEmpty() ? "" : " (revise: " + campos + ")");
        return volver(request, ra, mensaje);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String manejarParametroInvalido(
            MethodArgumentTypeMismatchException e, HttpServletRequest request, RedirectAttributes ra) {
        return volver(request, ra, "Valor inválido para el parámetro '" + e.getName() + "'");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public String manejarParametroFaltante(
            MissingServletRequestParameterException e, HttpServletRequest request, RedirectAttributes ra) {
        return volver(request, ra, "Falta el dato obligatorio '" + e.getParameterName() + "'");
    }

    @ExceptionHandler(DataAccessException.class)
    public String manejarErrorDeDatos(DataAccessException e, HttpServletRequest request, RedirectAttributes ra) {
        log.error("Error de acceso a datos en {}", request.getRequestURI(), e);
        return volver(request, ra, "No fue posible completar la operación por un error en los datos. Intente nuevamente.");
    }

    private static String volver(HttpServletRequest request, RedirectAttributes ra, String mensaje) {
        ra.addFlashAttribute("error", mensaje);
        return "redirect:" + destinoDeRetorno(request);
    }

    /** Referer (solo ruta y query, nunca otro sitio) -> sección del módulo -> raíz. */
    private static String destinoDeRetorno(HttpServletRequest request) {
        String contexto = request.getContextPath();
        String uri = request.getRequestURI();
        String actual = uri.startsWith(contexto) ? uri.substring(contexto.length()) : uri;

        String referer = rutaDelReferer(request, contexto);
        boolean esGet = "GET".equalsIgnoreCase(request.getMethod());
        // Un GET que falla no puede devolver a sí mismo (bucle de redirecciones).
        if (referer != null && !(esGet && referer.split("\\?", 2)[0].equals(actual))) {
            return referer;
        }
        int corte = actual.indexOf('/', 1);
        String seccion = corte > 0 ? actual.substring(0, corte) : actual;
        return !seccion.isEmpty() && !seccion.equals(actual) ? seccion : "/";
    }

    private static String rutaDelReferer(HttpServletRequest request, String contexto) {
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(referer.trim());
            String ruta = uri.getRawPath();
            if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(request.getServerName())) {
                return null;
            }
            if (ruta == null || !ruta.startsWith(contexto)) {
                return null;
            }
            ruta = ruta.substring(contexto.length());
            if (!ruta.startsWith("/") || ruta.startsWith("//")) {
                return null;
            }
            return uri.getRawQuery() == null ? ruta : ruta + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
