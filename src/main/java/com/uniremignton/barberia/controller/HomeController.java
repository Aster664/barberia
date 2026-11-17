package com.uniremignton.barberia.controller;

import com.uniremignton.barberia.service.BarberoService;
import com.uniremignton.barberia.service.CitaService;
import com.uniremignton.barberia.service.ClienteService;
import com.uniremignton.barberia.service.ProductoService;
import com.uniremignton.barberia.service.ServicioService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final BarberoService barberoService;
    private final ClienteService clienteService;
    private final ServicioService servicioService;
    private final CitaService citaService;
    private final ProductoService productoService;

    public HomeController(
            BarberoService barberoService,
            ClienteService clienteService,
            ServicioService servicioService,
            CitaService citaService,
            ProductoService productoService) {
        this.barberoService = barberoService;
        this.clienteService = clienteService;
        this.servicioService = servicioService;
        this.citaService = citaService;
        this.productoService = productoService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("totalBarberos", barberoService.listar().size());
        model.addAttribute("totalClientes", clienteService.listar().size());
        model.addAttribute("totalServicios", servicioService.listar().size());
        model.addAttribute("totalCitas", citaService.listar().size());
        model.addAttribute("totalProductos", productoService.listar().size());
        return "dashboard";
    }
}
