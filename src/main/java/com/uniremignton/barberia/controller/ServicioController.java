package com.uniremignton.barberia.controller;

import com.uniremignton.barberia.model.vo.EstadoServicio;
import com.uniremignton.barberia.model.vo.ServicioVO;
import com.uniremignton.barberia.service.ServicioService;
import java.math.BigDecimal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Vistas futuras: servicios/lista, servicios/form, servicios/detalle. */
@Controller
@RequestMapping("/servicios")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("servicios", servicioService.listar());
        return "servicios/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        return formulario(model, new ServicioVO());
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) Integer duracionMinutos,
            @RequestParam(required = false) BigDecimal precio,
            @RequestParam(required = false) EstadoServicio estado,
            RedirectAttributes ra) {
        servicioService.registrar(new ServicioVO(
                null, nombre, descripcion, duracionMinutos, precio, estado, null, null));
        ra.addFlashAttribute("mensaje", "Servicio registrado correctamente");
        return "redirect:/servicios";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable long id, Model model) {
        model.addAttribute("servicio", servicioService.buscarPorId(id));
        return "servicios/detalle";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable long id, Model model) {
        return formulario(model, servicioService.buscarPorId(id));
    }

    @PostMapping("/{id}/actualizar")
    public String actualizar(
            @PathVariable long id,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) Integer duracionMinutos,
            @RequestParam(required = false) BigDecimal precio,
            @RequestParam(required = false) EstadoServicio estado,
            RedirectAttributes ra) {
        servicioService.actualizar(new ServicioVO(
                id, nombre, descripcion, duracionMinutos, precio, estado, null, null));
        ra.addFlashAttribute("mensaje", "Servicio actualizado correctamente");
        return "redirect:/servicios";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable long id, RedirectAttributes ra) {
        servicioService.eliminar(id);
        ra.addFlashAttribute("mensaje", "Servicio eliminado correctamente");
        return "redirect:/servicios";
    }

    private String formulario(Model model, ServicioVO servicio) {
        model.addAttribute("servicio", servicio);
        model.addAttribute("estados", EstadoServicio.values());
        return "servicios/form";
    }
}
