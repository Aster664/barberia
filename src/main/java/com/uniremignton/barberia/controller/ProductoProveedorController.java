package com.uniremignton.barberia.controller;

import static com.uniremignton.barberia.controller.ControllerUtils.indexar;

import com.uniremignton.barberia.model.vo.ProductoProveedorVO;
import com.uniremignton.barberia.model.vo.ProductoVO;
import com.uniremignton.barberia.model.vo.ProveedorVO;
import com.uniremignton.barberia.service.ProductoProveedorService;
import com.uniremignton.barberia.service.ProductoService;
import com.uniremignton.barberia.service.ProveedorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Asociación producto-proveedor, administrable desde ambos lados.
 * Vistas futuras: producto-proveedores/por-producto, producto-proveedores/por-proveedor.
 */
@Controller
public class ProductoProveedorController {

    private final ProductoProveedorService productoProveedorService;
    private final ProductoService productoService;
    private final ProveedorService proveedorService;

    public ProductoProveedorController(
            ProductoProveedorService productoProveedorService,
            ProductoService productoService,
            ProveedorService proveedorService) {
        this.productoProveedorService = productoProveedorService;
        this.productoService = productoService;
        this.proveedorService = proveedorService;
    }

    // ---- Desde el producto ----

    @GetMapping("/productos/{idProducto}/proveedores")
    public String porProducto(@PathVariable long idProducto, Model model) {
        model.addAttribute("producto", productoService.buscarPorId(idProducto));
        model.addAttribute("asociaciones", productoProveedorService.listarPorProducto(idProducto));
        model.addAttribute("proveedoresPorId", indexar(proveedorService.listar(), ProveedorVO::idProveedor));
        return "producto-proveedores/por-producto";
    }

    @PostMapping("/productos/{idProducto}/proveedores/asociar")
    public String asociarDesdeProducto(
            @PathVariable long idProducto, @RequestParam(required = false) Long idProveedor, RedirectAttributes ra) {
        productoProveedorService.asociar(new ProductoProveedorVO(idProducto, idProveedor));
        ra.addFlashAttribute("mensaje", "Proveedor asociado al producto correctamente");
        return "redirect:/productos/" + idProducto + "/proveedores";
    }

    @PostMapping("/productos/{idProducto}/proveedores/{idProveedor}/desasociar")
    public String desasociarDesdeProducto(
            @PathVariable long idProducto, @PathVariable long idProveedor, RedirectAttributes ra) {
        productoProveedorService.desasociar(idProducto, idProveedor);
        ra.addFlashAttribute("mensaje", "Proveedor desasociado del producto correctamente");
        return "redirect:/productos/" + idProducto + "/proveedores";
    }

    // ---- Desde el proveedor ----

    @GetMapping("/proveedores/{idProveedor}/productos")
    public String porProveedor(@PathVariable long idProveedor, Model model) {
        model.addAttribute("proveedor", proveedorService.buscarPorId(idProveedor));
        model.addAttribute("asociaciones", productoProveedorService.listarPorProveedor(idProveedor));
        model.addAttribute("productosPorId", indexar(productoService.listar(), ProductoVO::idProducto));
        return "producto-proveedores/por-proveedor";
    }

    @PostMapping("/proveedores/{idProveedor}/productos/asociar")
    public String asociarDesdeProveedor(
            @PathVariable long idProveedor, @RequestParam(required = false) Long idProducto, RedirectAttributes ra) {
        productoProveedorService.asociar(new ProductoProveedorVO(idProducto, idProveedor));
        ra.addFlashAttribute("mensaje", "Producto asociado al proveedor correctamente");
        return "redirect:/proveedores/" + idProveedor + "/productos";
    }

    @PostMapping("/proveedores/{idProveedor}/productos/{idProducto}/desasociar")
    public String desasociarDesdeProveedor(
            @PathVariable long idProveedor, @PathVariable long idProducto, RedirectAttributes ra) {
        productoProveedorService.desasociar(idProducto, idProveedor);
        ra.addFlashAttribute("mensaje", "Producto desasociado del proveedor correctamente");
        return "redirect:/proveedores/" + idProveedor + "/productos";
    }
}
