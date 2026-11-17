package com.uniremignton.barberia.controller;

import static com.uniremignton.barberia.controller.ControllerUtils.indexar;

import com.uniremignton.barberia.model.vo.CitaServicioVO;
import com.uniremignton.barberia.model.vo.ServicioVO;
import com.uniremignton.barberia.service.CitaService;
import com.uniremignton.barberia.service.CitaServicioService;
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

/**
 * Servicios incluidos en una cita (cantidad y precio aplicado).
 * Vistas futuras: cita-servicios/lista, cita-servicios/form.
 * Si {@code precioAplicado} no se envía, el Service usa el precio actual del servicio (al agregar)
 * o conserva el existente (al actualizar).
 */
@Controller
@RequestMapping("/citas/{idCita}/servicios")
public class CitaServicioController {

    private final CitaServicioService citaServicioService;
    private final CitaService citaService;
    private final ServicioService servicioService;

    public CitaServicioController(
            CitaServicioService citaServicioService, CitaService citaService, ServicioService servicioService) {
        this.citaServicioService = citaServicioService;
        this.citaService = citaService;
        this.servicioService = servicioService;
    }

    @GetMapping
    public String listar(@PathVariable long idCita, Model model) {
        model.addAttribute("cita", citaService.buscarPorId(idCita));
        model.addAttribute("detalles", citaServicioService.listarPorCita(idCita));
        model.addAttribute("serviciosPorId", indexar(servicioService.listar(), ServicioVO::idServicio));
        model.addAttribute("total", citaServicioService.calcularTotal(idCita));
        return "cita-servicios/lista";
    }

    @PostMapping("/agregar")
    public String agregar(
            @PathVariable long idCita,
            @RequestParam(required = false) Long idServicio,
            @RequestParam(required = false) Integer cantidad,
            @RequestParam(required = false) BigDecimal precioAplicado,
            RedirectAttributes ra) {
        citaServicioService.asociar(new CitaServicioVO(idCita, idServicio, cantidad, precioAplicado));
        ra.addFlashAttribute("mensaje", "Servicio agregado a la cita correctamente");
        return "redirect:/citas/" + idCita + "/servicios";
    }

    @GetMapping("/{idServicio}/editar")
    public String editar(@PathVariable long idCita, @PathVariable long idServicio, Model model) {
        model.addAttribute("cita", citaService.buscarPorId(idCita));
        model.addAttribute("servicio", servicioService.buscarPorId(idServicio));
        model.addAttribute("detalle", citaServicioService.buscarPorId(idCita, idServicio));
        return "cita-servicios/form";
    }

    @PostMapping("/{idServicio}/actualizar")
    public String actualizar(
            @PathVariable long idCita,
            @PathVariable long idServicio,
            @RequestParam(required = false) Integer cantidad,
            @RequestParam(required = false) BigDecimal precioAplicado,
            RedirectAttributes ra) {
        citaServicioService.actualizar(new CitaServicioVO(idCita, idServicio, cantidad, precioAplicado));
        ra.addFlashAttribute("mensaje", "Servicio de la cita actualizado correctamente");
        return "redirect:/citas/" + idCita + "/servicios";
    }

    @PostMapping("/{idServicio}/eliminar")
    public String eliminar(@PathVariable long idCita, @PathVariable long idServicio, RedirectAttributes ra) {
        citaServicioService.desasociar(idCita, idServicio);
        ra.addFlashAttribute("mensaje", "Servicio quitado de la cita correctamente");
        return "redirect:/citas/" + idCita + "/servicios";
    }
}
