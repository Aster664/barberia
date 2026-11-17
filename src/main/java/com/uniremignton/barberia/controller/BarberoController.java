package com.uniremignton.barberia.controller;

import com.uniremignton.barberia.model.vo.BarberoVO;
import com.uniremignton.barberia.model.vo.EstadoBarbero;
import com.uniremignton.barberia.service.BarberoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Vistas futuras: barberos/lista, barberos/form, barberos/detalle. */
@Controller
@RequestMapping("/barberos")
public class BarberoController {

    private final BarberoService barberoService;

    public BarberoController(BarberoService barberoService) {
        this.barberoService = barberoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("barberos", barberoService.listar());
        return "barberos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        return formulario(model, new BarberoVO());
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(required = false) String nombres,
            @RequestParam(required = false) String apellidos,
            @RequestParam(required = false) String documentoIdentidad,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) LocalDate fechaContratacion,
            @RequestParam(required = false) BigDecimal porcentajeComision,
            @RequestParam(required = false) EstadoBarbero estado,
            RedirectAttributes ra) {
        barberoService.registrar(new BarberoVO(
                null, nombres, apellidos, documentoIdentidad, telefono, email,
                fechaContratacion, porcentajeComision, estado, null, null));
        ra.addFlashAttribute("mensaje", "Barbero registrado correctamente");
        return "redirect:/barberos";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable long id, Model model) {
        model.addAttribute("barbero", barberoService.buscarPorId(id));
        return "barberos/detalle";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable long id, Model model) {
        return formulario(model, barberoService.buscarPorId(id));
    }

    @PostMapping("/{id}/actualizar")
    public String actualizar(
            @PathVariable long id,
            @RequestParam(required = false) String nombres,
            @RequestParam(required = false) String apellidos,
            @RequestParam(required = false) String documentoIdentidad,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) LocalDate fechaContratacion,
            @RequestParam(required = false) BigDecimal porcentajeComision,
            @RequestParam(required = false) EstadoBarbero estado,
            RedirectAttributes ra) {
        barberoService.actualizar(new BarberoVO(
                id, nombres, apellidos, documentoIdentidad, telefono, email,
                fechaContratacion, porcentajeComision, estado, null, null));
        ra.addFlashAttribute("mensaje", "Barbero actualizado correctamente");
        return "redirect:/barberos";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable long id, RedirectAttributes ra) {
        barberoService.eliminar(id);
        ra.addFlashAttribute("mensaje", "Barbero eliminado correctamente");
        return "redirect:/barberos";
    }

    private String formulario(Model model, BarberoVO barbero) {
        model.addAttribute("barbero", barbero);
        model.addAttribute("estados", EstadoBarbero.values());
        return "barberos/form";
    }
}
