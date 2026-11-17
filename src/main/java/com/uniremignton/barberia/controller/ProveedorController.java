package com.uniremignton.barberia.controller;

import com.uniremignton.barberia.model.vo.ProveedorVO;
import com.uniremignton.barberia.service.ProveedorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Vistas futuras: proveedores/lista, proveedores/form, proveedores/detalle. */
@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("proveedores", proveedorService.listar());
        return "proveedores/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("proveedor", new ProveedorVO());
        return "proveedores/form";
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String direccion,
            RedirectAttributes ra) {
        proveedorService.registrar(new ProveedorVO(null, nombre, telefono, correo, direccion));
        ra.addFlashAttribute("mensaje", "Proveedor registrado correctamente");
        return "redirect:/proveedores";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable long id, Model model) {
        model.addAttribute("proveedor", proveedorService.buscarPorId(id));
        return "proveedores/detalle";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable long id, Model model) {
        model.addAttribute("proveedor", proveedorService.buscarPorId(id));
        return "proveedores/form";
    }

    @PostMapping("/{id}/actualizar")
    public String actualizar(
            @PathVariable long id,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String direccion,
            RedirectAttributes ra) {
        proveedorService.actualizar(new ProveedorVO(id, nombre, telefono, correo, direccion));
        ra.addFlashAttribute("mensaje", "Proveedor actualizado correctamente");
        return "redirect:/proveedores";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable long id, RedirectAttributes ra) {
        proveedorService.eliminar(id);
        ra.addFlashAttribute("mensaje", "Proveedor eliminado correctamente");
        return "redirect:/proveedores";
    }
}
