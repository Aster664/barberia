package com.uniremignton.barberia.controller;

import com.uniremignton.barberia.model.vo.EstadoPago;
import com.uniremignton.barberia.model.vo.MetodoPago;
import com.uniremignton.barberia.model.vo.PagoVO;
import com.uniremignton.barberia.service.CitaService;
import com.uniremignton.barberia.service.PagoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Vistas futuras: pagos/lista, pagos/form, pagos/detalle.
 * Las reglas de saldo, transiciones de estado y edición permitida viven en {@link PagoService}.
 */
@Controller
@RequestMapping("/pagos")
public class PagoController {

    private final PagoService pagoService;
    private final CitaService citaService;

    public PagoController(PagoService pagoService, CitaService citaService) {
        this.pagoService = pagoService;
        this.citaService = citaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("pagos", pagoService.listar());
        return "pagos/lista";
    }

    @GetMapping("/cita/{idCita}")
    public String listarPorCita(@PathVariable long idCita, Model model) {
        model.addAttribute("cita", citaService.buscarPorId(idCita));
        model.addAttribute("pagos", pagoService.listarPorCita(idCita));
        return "pagos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(@RequestParam(required = false) Long idCita, Model model) {
        return formulario(model, new PagoVO(null, idCita, null, null, null, null, null, null, null));
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(required = false) Long idCita,
            @RequestParam(required = false) BigDecimal monto,
            @RequestParam(required = false) BigDecimal propina,
            @RequestParam(required = false) MetodoPago metodoPago,
            @RequestParam(required = false) EstadoPago estado,
            @RequestParam(required = false) String referencia,
            @RequestParam(required = false) LocalDateTime fechaPago,
            RedirectAttributes ra) {
        PagoVO creado = pagoService.registrar(new PagoVO(
                null, idCita, monto, propina, metodoPago, estado, referencia, fechaPago, null));
        ra.addFlashAttribute("mensaje", "Pago registrado correctamente");
        return "redirect:/pagos/cita/" + creado.idCita();
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable long id, Model model) {
        PagoVO pago = pagoService.buscarPorId(id);
        model.addAttribute("pago", pago);
        model.addAttribute("cita", citaService.buscarPorId(pago.idCita()));
        model.addAttribute("estados", EstadoPago.values());
        return "pagos/detalle";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable long id, Model model) {
        return formulario(model, pagoService.buscarPorId(id));
    }

    @PostMapping("/{id}/actualizar")
    public String actualizar(
            @PathVariable long id,
            @RequestParam(required = false) Long idCita,
            @RequestParam(required = false) BigDecimal monto,
            @RequestParam(required = false) BigDecimal propina,
            @RequestParam(required = false) MetodoPago metodoPago,
            @RequestParam(required = false) EstadoPago estado,
            @RequestParam(required = false) String referencia,
            @RequestParam(required = false) LocalDateTime fechaPago,
            RedirectAttributes ra) {
        PagoVO actualizado = pagoService.actualizar(new PagoVO(
                id, idCita, monto, propina, metodoPago, estado, referencia, fechaPago, null));
        ra.addFlashAttribute("mensaje", "Pago actualizado correctamente");
        return "redirect:/pagos/cita/" + actualizado.idCita();
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(
            @PathVariable long id, @RequestParam(required = false) EstadoPago estado, RedirectAttributes ra) {
        PagoVO pago = pagoService.cambiarEstado(id, estado);
        ra.addFlashAttribute("mensaje", "El pago ahora está " + pago.estado());
        return "redirect:/pagos/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable long id, RedirectAttributes ra) {
        // La cita se consulta antes de borrar para poder volver a la lista de pagos de esa cita.
        long idCita = pagoService.buscarPorId(id).idCita();
        pagoService.eliminar(id);
        ra.addFlashAttribute("mensaje", "Pago eliminado correctamente");
        return "redirect:/pagos/cita/" + idCita;
    }

    private String formulario(Model model, PagoVO pago) {
        model.addAttribute("pago", pago);
        model.addAttribute("citas", citaService.listar());
        model.addAttribute("metodos", MetodoPago.values());
        model.addAttribute("estados", EstadoPago.values());
        return "pagos/form";
    }
}
