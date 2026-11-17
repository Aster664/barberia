package com.uniremignton.barberia.controller;

import static com.uniremignton.barberia.controller.ControllerUtils.indexar;

import com.uniremignton.barberia.model.vo.BarberoServicioVO;
import com.uniremignton.barberia.model.vo.BarberoVO;
import com.uniremignton.barberia.model.vo.ServicioVO;
import com.uniremignton.barberia.service.BarberoService;
import com.uniremignton.barberia.service.BarberoServicioService;
import com.uniremignton.barberia.service.ServicioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Asociación barbero-servicio, administrable desde ambos lados.
 * Vistas futuras: barbero-servicios/por-barbero, barbero-servicios/por-servicio.
 */
@Controller
public class BarberoServicioController {

    private final BarberoServicioService barberoServicioService;
    private final BarberoService barberoService;
    private final ServicioService servicioService;

    public BarberoServicioController(
            BarberoServicioService barberoServicioService,
            BarberoService barberoService,
            ServicioService servicioService) {
        this.barberoServicioService = barberoServicioService;
        this.barberoService = barberoService;
        this.servicioService = servicioService;
    }

    // ---- Desde el barbero ----

    @GetMapping("/barberos/{idBarbero}/servicios")
    public String porBarbero(@PathVariable long idBarbero, Model model) {
        model.addAttribute("barbero", barberoService.buscarPorId(idBarbero));
        model.addAttribute("asociaciones", barberoServicioService.listarPorBarbero(idBarbero));
        model.addAttribute("serviciosPorId", indexar(servicioService.listar(), ServicioVO::idServicio));
        return "barbero-servicios/por-barbero";
    }

    @PostMapping("/barberos/{idBarbero}/servicios/asociar")
    public String asociarDesdeBarbero(
            @PathVariable long idBarbero, @RequestParam(required = false) Long idServicio, RedirectAttributes ra) {
        barberoServicioService.asociar(new BarberoServicioVO(idBarbero, idServicio));
        ra.addFlashAttribute("mensaje", "Servicio asociado al barbero correctamente");
        return "redirect:/barberos/" + idBarbero + "/servicios";
    }

    @PostMapping("/barberos/{idBarbero}/servicios/{idServicio}/desasociar")
    public String desasociarDesdeBarbero(
            @PathVariable long idBarbero, @PathVariable long idServicio, RedirectAttributes ra) {
        barberoServicioService.desasociar(idBarbero, idServicio);
        ra.addFlashAttribute("mensaje", "Servicio desasociado del barbero correctamente");
        return "redirect:/barberos/" + idBarbero + "/servicios";
    }

    // ---- Desde el servicio ----

    @GetMapping("/servicios/{idServicio}/barberos")
    public String porServicio(@PathVariable long idServicio, Model model) {
        model.addAttribute("servicio", servicioService.buscarPorId(idServicio));
        model.addAttribute("asociaciones", barberoServicioService.listarPorServicio(idServicio));
        model.addAttribute("barberosPorId", indexar(barberoService.listar(), BarberoVO::idBarbero));
        return "barbero-servicios/por-servicio";
    }

    @PostMapping("/servicios/{idServicio}/barberos/asociar")
    public String asociarDesdeServicio(
            @PathVariable long idServicio, @RequestParam(required = false) Long idBarbero, RedirectAttributes ra) {
        barberoServicioService.asociar(new BarberoServicioVO(idBarbero, idServicio));
        ra.addFlashAttribute("mensaje", "Barbero asociado al servicio correctamente");
        return "redirect:/servicios/" + idServicio + "/barberos";
    }

    @PostMapping("/servicios/{idServicio}/barberos/{idBarbero}/desasociar")
    public String desasociarDesdeServicio(
            @PathVariable long idServicio, @PathVariable long idBarbero, RedirectAttributes ra) {
        barberoServicioService.desasociar(idBarbero, idServicio);
        ra.addFlashAttribute("mensaje", "Barbero desasociado del servicio correctamente");
        return "redirect:/servicios/" + idServicio + "/barberos";
    }
}
