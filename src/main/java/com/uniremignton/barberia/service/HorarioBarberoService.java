package com.uniremignton.barberia.service;

import com.uniremignton.barberia.model.dao.CitaDAO;
import com.uniremignton.barberia.model.dao.HorarioBarberoDAO;
import com.uniremignton.barberia.model.vo.CitaVO;
import com.uniremignton.barberia.model.vo.EstadoBarbero;
import com.uniremignton.barberia.model.vo.HorarioBarberoVO;
import com.uniremignton.barberia.service.exception.RecursoNoEncontradoException;
import com.uniremignton.barberia.service.exception.ReglaNegocioException;
import com.uniremignton.barberia.service.exception.ValidacionException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestiona los bloques de horario semanal de los barberos (dia_semana: 1 = lunes ... 7 = domingo). */
@Service
@Transactional(readOnly = true)
public class HorarioBarberoService {

    private final HorarioBarberoDAO horarioDAO;
    private final CitaDAO citaDAO;
    private final BarberoService barberoService;

    public HorarioBarberoService(
            HorarioBarberoDAO horarioDAO, CitaDAO citaDAO, BarberoService barberoService) {
        this.horarioDAO = horarioDAO;
        this.citaDAO = citaDAO;
        this.barberoService = barberoService;
    }

    @Transactional
    public HorarioBarberoVO registrar(HorarioBarberoVO horario) {
        Validador.requerido(horario, "horario");
        HorarioBarberoVO nuevo = normalizar(horario, null);
        validarBloque(nuevo);
        return horarioDAO.crear(nuevo);
    }

    /** Actualiza un bloque; no puede dejar sin cobertura a citas vigentes que ya estaban dentro de él. */
    @Transactional
    public HorarioBarberoVO actualizar(HorarioBarberoVO horario) {
        Validador.requerido(horario, "horario");
        long id = Validador.idPositivo(horario.idHorario(), "idHorario");
        HorarioBarberoVO actual = buscarPorId(id);
        HorarioBarberoVO actualizado = normalizar(horario, id);
        validarBloque(actualizado);
        validarCitasSiguenCubiertas(actual, actualizado);
        if (!horarioDAO.actualizar(actualizado)) {
            throw new RecursoNoEncontradoException("Horario", id);
        }
        return buscarPorId(id);
    }

    public HorarioBarberoVO buscarPorId(long idHorario) {
        Validador.idPositivo(idHorario, "idHorario");
        try {
            return horarioDAO.buscarPorId(idHorario);
        } catch (EmptyResultDataAccessException e) {
            throw new RecursoNoEncontradoException("Horario", idHorario);
        }
    }

    public List<HorarioBarberoVO> listar() {
        return horarioDAO.listar();
    }

    public List<HorarioBarberoVO> listarPorBarbero(long idBarbero) {
        barberoService.buscarPorId(idBarbero);
        return horarioDAO.listarPorBarbero(idBarbero);
    }

    /** Elimina un bloque; no puede dejar sin cobertura a citas vigentes que estaban dentro de él. */
    @Transactional
    public void eliminar(long idHorario) {
        HorarioBarberoVO actual = buscarPorId(idHorario);
        validarCitasSiguenCubiertas(actual, null);
        if (!horarioDAO.eliminar(idHorario)) {
            throw new RecursoNoEncontradoException("Horario", idHorario);
        }
    }

    /** Verifica que el intervalo quede dentro de un único bloque de trabajo del barbero. */
    public void validarDentroDeHorario(long idBarbero, LocalDateTime inicio, LocalDateTime fin) {
        boolean cubierto = horarioDAO.listarPorBarbero(idBarbero).stream()
                .anyMatch(horario -> cubre(horario, inicio, fin));
        if (!cubierto) {
            throw new ReglaNegocioException(
                    "El barbero no atiende en el horario solicitado; revise sus horarios de trabajo");
        }
    }

    private void validarBloque(HorarioBarberoVO horario) {
        if (barberoService.buscarPorId(horario.idBarbero()).estado() == EstadoBarbero.INACTIVO) {
            throw new ReglaNegocioException("No se pueden definir horarios para un barbero INACTIVO");
        }
        for (HorarioBarberoVO otro : horarioDAO.listarPorBarbero(horario.idBarbero())) {
            boolean mismoBloque = otro.idHorario().equals(horario.idHorario());
            if (!mismoBloque
                    && otro.diaSemana().equals(horario.diaSemana())
                    && horario.horaInicio().isBefore(otro.horaFin())
                    && horario.horaFin().isAfter(otro.horaInicio())) {
                throw new ReglaNegocioException("El horario se cruza con otro bloque del barbero ("
                        + otro.horaInicio() + " - " + otro.horaFin() + ") ese mismo día");
            }
        }
    }

    private void validarCitasSiguenCubiertas(HorarioBarberoVO original, HorarioBarberoVO nuevo) {
        List<HorarioBarberoVO> restantes = new ArrayList<>();
        for (HorarioBarberoVO horario : horarioDAO.listarPorBarbero(original.idBarbero())) {
            if (!horario.idHorario().equals(original.idHorario())) {
                restantes.add(horario);
            }
        }
        if (nuevo != null && nuevo.idBarbero().equals(original.idBarbero())) {
            restantes.add(nuevo);
        }
        LocalDateTime ahora = LocalDateTime.now();
        for (CitaVO cita : citaDAO.listarPorBarbero(original.idBarbero())) {
            boolean vigenteFutura = cita.fechaHoraInicio().isAfter(ahora)
                    && CitaService.ESTADOS_VIGENTES.contains(cita.estado());
            if (vigenteFutura
                    && cubre(original, cita.fechaHoraInicio(), cita.fechaHoraFin())
                    && restantes.stream().noneMatch(h -> cubre(h, cita.fechaHoraInicio(), cita.fechaHoraFin()))) {
                throw new ReglaNegocioException(
                        "Existen citas vigentes dentro de este horario; reprográmelas o cancélelas antes de modificarlo");
            }
        }
    }

    private static boolean cubre(HorarioBarberoVO horario, LocalDateTime inicio, LocalDateTime fin) {
        return inicio.toLocalDate().equals(fin.toLocalDate())
                && inicio.getDayOfWeek().getValue() == horario.diaSemana()
                && !inicio.toLocalTime().isBefore(horario.horaInicio())
                && !fin.toLocalTime().isAfter(horario.horaFin());
    }

    private static HorarioBarberoVO normalizar(HorarioBarberoVO h, Long id) {
        Validador.idPositivo(h.idBarbero(), "idBarbero");
        int dia = Validador.enteroEnRango(h.diaSemana(), "diaSemana", 1, 7);
        Validador.requerido(h.horaInicio(), "horaInicio");
        Validador.requerido(h.horaFin(), "horaFin");
        if (!h.horaInicio().isBefore(h.horaFin())) {
            throw new ValidacionException("La hora de inicio debe ser anterior a la hora de fin");
        }
        return new HorarioBarberoVO(id, h.idBarbero(), dia, h.horaInicio(), h.horaFin());
    }
}
