package com.uniremignton.barberia.controller;

import com.uniremignton.barberia.model.vo.ProductoVO;
import com.uniremignton.barberia.service.ProductoService;
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
 * Vistas futuras: productos/lista, productos/form, productos/detalle.
 * {@code cantidadDisponible} solo se usa como stock inicial al registrar; después el stock cambia
 * únicamente con movimientos de inventario (ver {@link InventarioController}).
 */
@Controller
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("productos", productoService.listar());
        return "productos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("producto", new ProductoVO());
        return "productos/form";
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) BigDecimal precioCompra,
            @RequestParam(required = false) BigDecimal precioVenta,
            @RequestParam(required = false) Integer cantidadDisponible,
            @RequestParam(required = false) Integer stockMinimo,
            RedirectAttributes ra) {
        productoService.registrar(new ProductoVO(
                null, nombre, descripcion, precioCompra, precioVenta, cantidadDisponible, stockMinimo));
        ra.addFlashAttribute("mensaje", "Producto registrado correctamente");
        return "redirect:/productos";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable long id, Model model) {
        model.addAttribute("producto", productoService.buscarPorId(id));
        return "productos/detalle";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable long id, Model model) {
        model.addAttribute("producto", productoService.buscarPorId(id));
        return "productos/form";
    }

    @PostMapping("/{id}/actualizar")
    public String actualizar(
            @PathVariable long id,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) BigDecimal precioCompra,
            @RequestParam(required = false) BigDecimal precioVenta,
            @RequestParam(required = false) Integer stockMinimo,
            RedirectAttributes ra) {
        // El stock no se edita aquí: el Service conserva el actual (cantidadDisponible = null).
        productoService.actualizar(new ProductoVO(
                id, nombre, descripcion, precioCompra, precioVenta, null, stockMinimo));
        ra.addFlashAttribute("mensaje", "Producto actualizado correctamente");
        return "redirect:/productos";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable long id, RedirectAttributes ra) {
        productoService.eliminar(id);
        ra.addFlashAttribute("mensaje", "Producto eliminado correctamente");
        return "redirect:/productos";
    }
}
