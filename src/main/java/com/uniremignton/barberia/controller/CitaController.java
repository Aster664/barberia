package com.uniremignton.barberia.controller;

import static com.uniremignton.barberia.controller.ControllerUtils.indexar;

import com.uniremignton.barberia.model.vo.BarberoVO;
import com.uniremignton.barberia.model.vo.CitaVO;
import com.uniremignton.barberia.model.vo.ClienteVO;
import com.uniremignton.barberia.model.vo.EstadoCita;
import com.uniremignton.barberia.model.vo.ServicioVO;
import com.uniremignton.barberia.service.BarberoService;
import com.uniremignton.barberia.service.CitaService;
import com.uniremignton.barberia.service.CitaServicioService;
import com.uniremignton.barberia.service.ClienteService;
import com.uniremignton.barberia.service.ServicioService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Vistas futuras: citas/lista, citas/form, citas/detalle, citas/reprogramar.
 * Las validaciones de horario, cruces, transiciones de estado y servicios viven en {@link CitaService};
 * aquí solo se compone el VO y se delega. Los servicios de una cita se administran en
 * {@link CitaServicioController} (/citas/{idCita}/servicios); el detalle los muestra con
 * {@link CitaServicioService}.
 * Fechas: formato ISO de {@code datetime-local} (yyyy-MM-dd'T'HH:mm).
 */
@Controller
@RequestMapping("/citas")
public class CitaController {

    private final CitaService citaService;
    private final ClienteService clienteService;
    private final BarberoService barberoService;
    private final ServicioService servicioService;
    private final CitaServicioService citaServicioService;

    public CitaController(
            CitaService citaService,
            ClienteService clienteService,
            BarberoService barberoService,
            ServicioService servicioService,
            CitaServicioService citaServicioService) {
        this.citaService = citaService;
        this.clienteService = clienteService;
        this.barberoService = barberoService;
        this.servicioService = servicioService;
        this.citaServicioService = citaServicioService;
    }

    // ---- Consultas ----

    @GetMapping
    public String listar(Model model) {
        return lista(model, citaService.listar());
    }

    @GetMapping("/cliente/{idCliente}")
    public String listarPorCliente(@PathVariable long idCliente, Model model) {
        model.addAttribute("cliente", clienteService.buscarPorId(idCliente));
        return lista(model, citaService.listarPorCliente(idCliente));
    }

    @GetMapping("/barbero/{idBarbero}")
    public String listarPorBarbero(@PathVariable long idBarbero, Model model) {
        model.addAttribute("barbero", barberoService.buscarPorId(idBarbero));
        return lista(model, citaService.listarPorBarbero(idBarbero));
    }

    /** Citas cuyo inicio está en [desde, hasta). Sin parámetros solo muestra el formulario de búsqueda. */
    @GetMapping("/rango")
    public String listarPorRango(
            @RequestParam(required = false) LocalDateTime desde,
            @RequestParam(required = false) LocalDateTime hasta,
            Model model) {
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        boolean buscar = desde != null || hasta != null;
        return lista(model, buscar ? citaService.listarPorRango(desde, hasta) : List.of());
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable long id, Model model) {
        CitaVO cita = citaService.buscarPorId(id);
        model.addAttribute("cita", cita);
        model.addAttribute("cliente", clienteService.buscarPorId(cita.idCliente()));
        model.addAttribute("barbero", barberoService.buscarPorId(cita.idBarbero()));
        model.addAttribute("detalles", citaServicioService.listarPorCita(id));
        model.addAttribute("serviciosPorId", indexar(servicioService.listar(), ServicioVO::idServicio));
        model.addAttribute("total", citaServicioService.calcularTotal(id));
        model.addAttribute("estados", EstadoCita.values());
        return "citas/detalle";
    }

    // ---- Registro y edición ----

    @GetMapping("/nuevo")
    public String nuevo(
            @RequestParam(required = false) Long idCliente,
            @RequestParam(required = false) Long idBarbero,
            Model model) {
        return formulario(model, new CitaVO(null, idCliente, idBarbero, null, null, null, null, null, null));
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(required = false) Long idCliente,
            @RequestParam(required = false) Long idBarbero,
            @RequestParam(required = false) LocalDateTime fechaHoraInicio,
            @RequestParam(required = false) LocalDateTime fechaHoraFin,
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false) String observaciones,
            RedirectAttributes ra) {
        CitaVO creada = citaService.registrar(new CitaVO(
                null, idCliente, idBarbero, fechaHoraInicio, fechaHoraFin, estado, observaciones, null, null));
        ra.addFlashAttribute("mensaje", "Cita registrada correctamente");
        return "redirect:/citas/" + creada.idCita();
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable long id, Model model) {
        return formulario(model, citaService.buscarPorId(id));
    }

    @PostMapping("/{id}/actualizar")
    public String actualizar(
            @PathVariable long id,
            @RequestParam(required = false) Long idCliente,
            @RequestParam(required = false) Long idBarbero,
            @RequestParam(required = false) LocalDateTime fechaHoraInicio,
            @RequestParam(required = false) LocalDateTime fechaHoraFin,
            @RequestParam(required = false) EstadoCita estado,
            @RequestParam(required = false) String observaciones,
            RedirectAttributes ra) {
        citaService.actualizar(new CitaVO(
                id, idCliente, idBarbero, fechaHoraInicio, fechaHoraFin, estado, observaciones, null, null));
        ra.addFlashAttribute("mensaje", "Cita actualizada correctamente");
        return "redirect:/citas/" + id;
    }

    // ---- Reprogramación y estados ----

    @GetMapping("/{id}/reprogramar")
    public String formularioReprogramar(@PathVariable long id, Model model) {
        model.addAttribute("cita", citaService.buscarPorId(id));
        model.addAttribute("barberos", barberoService.listar());
        return "citas/reprogramar";
    }

    /** Cambia fecha/hora (y opcionalmente el barbero) conservando cliente, estado y observaciones. */
    @PostMapping("/{id}/reprogramar")
    public String reprogramar(
            @PathVariable long id,
            @RequestParam(required = false) LocalDateTime fechaHoraInicio,
            @RequestParam(required = false) LocalDateTime fechaHoraFin,
            @RequestParam(required = false) Long idBarbero,
            RedirectAttributes ra) {
        CitaVO actual = citaService.buscarPorId(id);
        citaService.actualizar(new CitaVO(
                id, actual.idCliente(), idBarbero != null ? idBarbero : actual.idBarbero(),
                fechaHoraInicio, fechaHoraFin, actual.estado(), actual.observaciones(), null, null));
        ra.addFlashAttribute("mensaje", "Cita reprogramada correctamente");
        return "redirect:/citas/" + id;
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(
            @PathVariable long id, @RequestParam(required = false) EstadoCita estado, RedirectAttributes ra) {
        CitaVO cita = citaService.cambiarEstado(id, estado);
        ra.addFlashAttribute("mensaje", "La cita ahora está " + cita.estado());
        return "redirect:/citas/" + id;
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable long id, RedirectAttributes ra) {
        citaService.eliminar(id);
        ra.addFlashAttribute("mensaje", "Cita eliminada correctamente");
        return "redirect:/citas";
    }

    // ---- Auxiliares ----

    private String lista(Model model, List<CitaVO> citas) {
        model.addAttribute("citas", citas);
        model.addAttribute("clientesPorId", indexar(clienteService.listar(), ClienteVO::idCliente));
        model.addAttribute("barberosPorId", indexar(barberoService.listar(), BarberoVO::idBarbero));
        return "citas/lista";
    }

    private String formulario(Model model, CitaVO cita) {
        model.addAttribute("cita", cita);
        model.addAttribute("clientes", clienteService.listar());
        model.addAttribute("barberos", barberoService.listar());
        model.addAttribute("estados", EstadoCita.values());
        return "citas/form";
    }
}
