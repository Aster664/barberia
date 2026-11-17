package com.uniremignton.barberia.controller;

import static com.uniremignton.barberia.controller.ControllerUtils.indexar;

import com.uniremignton.barberia.model.vo.BarberoVO;
import com.uniremignton.barberia.model.vo.HorarioBarberoVO;
import com.uniremignton.barberia.service.BarberoService;
import com.uniremignton.barberia.service.HorarioBarberoService;
import java.time.LocalTime;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Vistas futuras: horarios/lista, horarios/form.
 * {@code diaSemana} usa la convención del Service: 1 = lunes ... 7 = domingo.
 */
@Controller
@RequestMapping("/horarios")
public class HorarioBarberoController {

    private final HorarioBarberoService horarioService;
    private final BarberoService barberoService;

    public HorarioBarberoController(HorarioBarberoService horarioService, BarberoService barberoService) {
        this.horarioService = horarioService;
        this.barberoService = barberoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("horarios", horarioService.listar());
        model.addAttribute("barberosPorId", indexar(barberoService.listar(), BarberoVO::idBarbero));
        return "horarios/lista";
    }

    @GetMapping("/barbero/{idBarbero}")
    public String listarPorBarbero(@PathVariable long idBarbero, Model model) {
        model.addAttribute("barbero", barberoService.buscarPorId(idBarbero));
        model.addAttribute("horarios", horarioService.listarPorBarbero(idBarbero));
        model.addAttribute("barberosPorId", indexar(barberoService.listar(), BarberoVO::idBarbero));
        return "horarios/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(required = false) Long idBarbero, Model model) {
        return formulario(model, new HorarioBarberoVO(null, idBarbero, null, null, null));
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(required = false) Long idBarbero,
            @RequestParam(required = false) Integer diaSemana,
            @RequestParam(required = false) LocalTime horaInicio,
            @RequestParam(required = false) LocalTime horaFin,
            RedirectAttributes ra) {
        HorarioBarberoVO creado = horarioService.registrar(
                new HorarioBarberoVO(null, idBarbero, diaSemana, horaInicio, horaFin));
        ra.addFlashAttribute("mensaje", "Horario registrado correctamente");
        return "redirect:/horarios/barbero/" + creado.idBarbero();
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable long id, Model model) {
        return formulario(model, horarioService.buscarPorId(id));
    }

    @PostMapping("/{id}/actualizar")
    public String actualizar(
            @PathVariable long id,
            @RequestParam(required = false) Long idBarbero,
            @RequestParam(required = false) Integer diaSemana,
            @RequestParam(required = false) LocalTime horaInicio,
            @RequestParam(required = false) LocalTime horaFin,
            RedirectAttributes ra) {
        HorarioBarberoVO actualizado = horarioService.actualizar(
                new HorarioBarberoVO(id, idBarbero, diaSemana, horaInicio, horaFin));
        ra.addFlashAttribute("mensaje", "Horario actualizado correctamente");
        return "redirect:/horarios/barbero/" + actualizado.idBarbero();
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable long id, RedirectAttributes ra) {
        // El barbero se consulta antes de borrar para poder volver a su lista de horarios.
        long idBarbero = horarioService.buscarPorId(id).idBarbero();
        horarioService.eliminar(id);
        ra.addFlashAttribute("mensaje", "Horario eliminado correctamente");
        return "redirect:/horarios/barbero/" + idBarbero;
    }

    private String formulario(Model model, HorarioBarberoVO horario) {
        model.addAttribute("horario", horario);
        model.addAttribute("barberos", barberoService.listar());
        return "horarios/form";
    }
}
