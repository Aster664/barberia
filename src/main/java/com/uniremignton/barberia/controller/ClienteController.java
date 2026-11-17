package com.uniremignton.barberia.controller;

import com.uniremignton.barberia.model.vo.ClienteVO;
import com.uniremignton.barberia.model.vo.EstadoCliente;
import com.uniremignton.barberia.service.ClienteService;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Vistas futuras: clientes/lista, clientes/form, clientes/detalle. */
@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", clienteService.listar());
        return "clientes/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        return formulario(model, new ClienteVO());
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(required = false) String nombres,
            @RequestParam(required = false) String apellidos,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) LocalDate fechaNacimiento,
            @RequestParam(required = false) String notas,
            @RequestParam(required = false) EstadoCliente estado,
            RedirectAttributes ra) {
        clienteService.registrar(new ClienteVO(
                null, nombres, apellidos, telefono, email, fechaNacimiento, notas, estado, null, null));
        ra.addFlashAttribute("mensaje", "Cliente registrado correctamente");
        return "redirect:/clientes";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable long id, Model model) {
        model.addAttribute("cliente", clienteService.buscarPorId(id));
        return "clientes/detalle";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable long id, Model model) {
        return formulario(model, clienteService.buscarPorId(id));
    }

    @PostMapping("/{id}/actualizar")
    public String actualizar(
            @PathVariable long id,
            @RequestParam(required = false) String nombres,
            @RequestParam(required = false) String apellidos,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) LocalDate fechaNacimiento,
            @RequestParam(required = false) String notas,
            @RequestParam(required = false) EstadoCliente estado,
            RedirectAttributes ra) {
        clienteService.actualizar(new ClienteVO(
                id, nombres, apellidos, telefono, email, fechaNacimiento, notas, estado, null, null));
        ra.addFlashAttribute("mensaje", "Cliente actualizado correctamente");
        return "redirect:/clientes";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable long id, RedirectAttributes ra) {
        clienteService.eliminar(id);
        ra.addFlashAttribute("mensaje", "Cliente eliminado correctamente");
        return "redirect:/clientes";
    }

    private String formulario(Model model, ClienteVO cliente) {
        model.addAttribute("cliente", cliente);
        model.addAttribute("estados", EstadoCliente.values());
        return "clientes/form";
    }
}
