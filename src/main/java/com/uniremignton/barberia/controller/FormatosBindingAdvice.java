package com.uniremignton.barberia.controller;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.function.Function;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Configura el enlace de datos de todos los controladores sin tocar los VO ni la configuración:
 * <ul>
 *   <li>Los textos vacíos llegan como {@code null} (campos opcionales).</li>
 *   <li>Las fechas y horas se reciben en formato ISO, el que envían los inputs HTML:
 *       {@code yyyy-MM-dd}, {@code yyyy-MM-dd'T'HH:mm} y {@code HH:mm}.</li>
 * </ul>
 * Los VO no llevan {@code @DateTimeFormat}, por eso el formato se registra aquí.
 */
@ControllerAdvice
public class FormatosBindingAdvice {

    @InitBinder
    public void registrarFormatos(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        binder.registerCustomEditor(LocalDate.class, new EditorTemporal<>(LocalDate::parse));
        binder.registerCustomEditor(LocalDateTime.class, new EditorTemporal<>(LocalDateTime::parse));
        binder.registerCustomEditor(LocalTime.class, new EditorTemporal<>(LocalTime::parse));
    }

    private static final class EditorTemporal<T> extends PropertyEditorSupport {

        private final Function<String, T> lector;

        private EditorTemporal(Function<String, T> lector) {
            this.lector = lector;
        }

        @Override
        public void setAsText(String texto) {
            if (texto == null || texto.isBlank()) {
                setValue(null);
                return;
            }
            try {
                setValue(lector.apply(texto.trim()));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Formato de fecha u hora inválido: " + texto, e);
            }
        }

        @Override
        public String getAsText() {
            Object valor = getValue();
            return valor == null ? "" : valor.toString();
        }
    }
}
