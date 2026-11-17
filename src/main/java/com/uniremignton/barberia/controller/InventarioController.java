package com.uniremignton.barberia.controller;

import static com.uniremignton.barberia.controller.ControllerUtils.indexar;

import com.uniremignton.barberia.model.vo.MovimientoInventarioVO;
import com.uniremignton.barberia.model.vo.ProductoVO;
import com.uniremignton.barberia.model.vo.TipoMovimientoInventario;
import com.uniremignton.barberia.service.InventarioService;
import com.uniremignton.barberia.service.ProductoService;
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
 * Vistas futuras: inventario/lista, inventario/movimientos, inventario/movimiento-form,
 * inventario/movimiento-detalle.
 * Todo cambio de stock pasa por {@link InventarioService}; {@link ProductoService} solo se usa para
 * mostrar el inventario (productos y su stock actual) y resolver nombres.
 */
@Controller
@RequestMapping("/inventario")
public class InventarioController {

    private final InventarioService inventarioService;
    private final ProductoService productoService;

    public InventarioController(InventarioService inventarioService, ProductoService productoService) {
        this.inventarioService = inventarioService;
        this.productoService = productoService;
    }

    /** Inventario actual: productos con su cantidad disponible y stock mínimo. */
    @GetMapping
    public String inventario(Model model) {
        model.addAttribute("productos", productoService.listar());
        return "inventario/lista";
    }

    @GetMapping("/movimientos")
    public String movimientos(Model model) {
        return listaMovimientos(model, inventarioService.listar());
    }

    @GetMapping("/movimientos/producto/{idProducto}")
    public String movimientosPorProducto(@PathVariable long idProducto, Model model) {
        model.addAttribute("producto", productoService.buscarPorId(idProducto));
        return listaMovimientos(model, inventarioService.listarPorProducto(idProducto));
    }

    @GetMapping("/movimientos/nuevo")
    public String nuevoMovimiento(
            @RequestParam(required = false) Long idProducto,
            @RequestParam(required = false) TipoMovimientoInventario tipo,
            Model model) {
        model.addAttribute("movimiento", new MovimientoInventarioVO(null, idProducto, null, null, tipo));
        model.addAttribute("productos", productoService.listar());
        model.addAttribute("tipos", TipoMovimientoInventario.values());
        return "inventario/movimiento-form";
    }

    @PostMapping("/movimientos/guardar")
    public String guardarMovimiento(
            @RequestParam(required = false) Long idProducto,
            @RequestParam(required = false) Integer cantidad,
            @RequestParam(required = false) LocalDateTime fecha,
            @RequestParam(required = false) TipoMovimientoInventario tipo,
            RedirectAttributes ra) {
        MovimientoInventarioVO registrado = inventarioService.registrarMovimiento(
                new MovimientoInventarioVO(null, idProducto, cantidad, fecha, tipo));
        ra.addFlashAttribute("mensaje", "Movimiento de " + registrado.tipo() + " registrado correctamente");
        return "redirect:/inventario";
    }

    @GetMapping("/movimientos/{id}")
    public String detalleMovimiento(@PathVariable long id, Model model) {
        MovimientoInventarioVO movimiento = inventarioService.buscarPorId(id);
        model.addAttribute("movimiento", movimiento);
        model.addAttribute("producto", productoService.buscarPorId(movimiento.idProducto()));
        return "inventario/movimiento-detalle";
    }

    private String listaMovimientos(Model model, List<MovimientoInventarioVO> movimientos) {
        model.addAttribute("movimientos", movimientos);
        model.addAttribute("productosPorId", indexar(productoService.listar(), ProductoVO::idProducto));
        return "inventario/movimientos";
    }
}
