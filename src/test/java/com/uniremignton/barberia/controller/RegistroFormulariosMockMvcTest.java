package com.uniremignton.barberia.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.uniremignton.barberia.model.vo.BarberoVO;
import com.uniremignton.barberia.model.vo.CitaVO;
import com.uniremignton.barberia.model.vo.ClienteVO;
import com.uniremignton.barberia.model.vo.EstadoBarbero;
import com.uniremignton.barberia.model.vo.EstadoCita;
import com.uniremignton.barberia.model.vo.EstadoCliente;
import com.uniremignton.barberia.model.vo.EstadoPago;
import com.uniremignton.barberia.model.vo.EstadoServicio;
import com.uniremignton.barberia.model.vo.HorarioBarberoVO;
import com.uniremignton.barberia.model.vo.MetodoPago;
import com.uniremignton.barberia.model.vo.MovimientoInventarioVO;
import com.uniremignton.barberia.model.vo.PagoVO;
import com.uniremignton.barberia.model.vo.ProductoVO;
import com.uniremignton.barberia.model.vo.ProveedorVO;
import com.uniremignton.barberia.model.vo.ServicioVO;
import com.uniremignton.barberia.model.vo.TipoMovimientoInventario;
import com.uniremignton.barberia.service.BarberoService;
import com.uniremignton.barberia.service.CitaService;
import com.uniremignton.barberia.service.CitaServicioService;
import com.uniremignton.barberia.service.ClienteService;
import com.uniremignton.barberia.service.HorarioBarberoService;
import com.uniremignton.barberia.service.InventarioService;
import com.uniremignton.barberia.service.PagoService;
import com.uniremignton.barberia.service.ProductoService;
import com.uniremignton.barberia.service.ProveedorService;
import com.uniremignton.barberia.service.ServicioService;
import com.uniremignton.barberia.service.exception.ReglaNegocioException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Comprueba que lo que envía un formulario HTML (parámetros de la petición) llega a cada Service
 * con los valores y tipos correctos. Los Service se simulan con Mockito y se captura el VO recibido,
 * así que no hace falta base de datos. Se usa standaloneSetup con los mismos advice de la aplicación.
 */
class RegistroFormulariosMockMvcTest {

    private static MockMvc mvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new FormatosBindingAdvice(), new GlobalExceptionHandler())
                .build();
    }

    @Test
    void barbero_registrar_entregaTodosLosCamposAlService() throws Exception {
        BarberoService service = mock(BarberoService.class);

        mvc(new BarberoController(service))
                .perform(post("/barberos/guardar")
                        .param("nombres", "  Juan ")
                        .param("apellidos", "Pérez")
                        .param("documentoIdentidad", "1020304050")
                        .param("telefono", "")              // opcional vacío -> null
                        .param("email", "juan@barberia.com")
                        .param("fechaContratacion", "2026-01-15")
                        .param("porcentajeComision", "30.50")
                        .param("estado", "ACTIVO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/barberos"))
                .andExpect(flash().attribute("mensaje", "Barbero registrado correctamente"));

        ArgumentCaptor<BarberoVO> captor = ArgumentCaptor.forClass(BarberoVO.class);
        verify(service).registrar(captor.capture());
        BarberoVO vo = captor.getValue();
        assertNull(vo.idBarbero());
        assertEquals("Juan", vo.nombres());
        assertEquals("Pérez", vo.apellidos());
        assertEquals("1020304050", vo.documentoIdentidad());
        assertNull(vo.telefono());
        assertEquals("juan@barberia.com", vo.email());
        assertEquals(LocalDate.of(2026, 1, 15), vo.fechaContratacion());
        assertEquals(new BigDecimal("30.50"), vo.porcentajeComision());
        assertEquals(EstadoBarbero.ACTIVO, vo.estado());
    }

    @Test
    void cliente_registrar_entregaTodosLosCamposAlService() throws Exception {
        ClienteService service = mock(ClienteService.class);

        mvc(new ClienteController(service))
                .perform(post("/clientes/guardar")
                        .param("nombres", "Ana")
                        .param("apellidos", "Gómez")
                        .param("telefono", "3001234567")
                        .param("email", "")
                        .param("fechaNacimiento", "1990-05-20")
                        .param("notas", "Prefiere corte clásico")
                        .param("estado", "ACTIVO"))
                .andExpect(redirectedUrl("/clientes"));

        ArgumentCaptor<ClienteVO> captor = ArgumentCaptor.forClass(ClienteVO.class);
        verify(service).registrar(captor.capture());
        ClienteVO vo = captor.getValue();
        assertEquals("Ana", vo.nombres());
        assertEquals("Gómez", vo.apellidos());
        assertEquals("3001234567", vo.telefono());
        assertNull(vo.email());
        assertEquals(LocalDate.of(1990, 5, 20), vo.fechaNacimiento());
        assertEquals("Prefiere corte clásico", vo.notas());
        assertEquals(EstadoCliente.ACTIVO, vo.estado());
    }

    @Test
    void servicio_registrar_entregaTodosLosCamposAlService() throws Exception {
        ServicioService service = mock(ServicioService.class);

        mvc(new ServicioController(service))
                .perform(post("/servicios/guardar")
                        .param("nombre", "Corte clásico")
                        .param("descripcion", "")
                        .param("duracionMinutos", "45")
                        .param("precio", "25000.00")
                        .param("estado", "ACTIVO"))
                .andExpect(redirectedUrl("/servicios"));

        ArgumentCaptor<ServicioVO> captor = ArgumentCaptor.forClass(ServicioVO.class);
        verify(service).registrar(captor.capture());
        ServicioVO vo = captor.getValue();
        assertEquals("Corte clásico", vo.nombre());
        assertNull(vo.descripcion());
        assertEquals(45, vo.duracionMinutos());
        assertEquals(new BigDecimal("25000.00"), vo.precio());
        assertEquals(EstadoServicio.ACTIVO, vo.estado());
    }

    @Test
    void proveedor_registrar_entregaTodosLosCamposAlService() throws Exception {
        ProveedorService service = mock(ProveedorService.class);

        mvc(new ProveedorController(service))
                .perform(post("/proveedores/guardar")
                        .param("nombre", "Distribuidora Norte")
                        .param("telefono", "6015551234")
                        .param("correo", "ventas@norte.com")
                        .param("direccion", "Calle 10 # 5-20"))
                .andExpect(redirectedUrl("/proveedores"));

        ArgumentCaptor<ProveedorVO> captor = ArgumentCaptor.forClass(ProveedorVO.class);
        verify(service).registrar(captor.capture());
        ProveedorVO vo = captor.getValue();
        assertEquals("Distribuidora Norte", vo.nombre());
        assertEquals("6015551234", vo.telefono());
        assertEquals("ventas@norte.com", vo.correo());
        assertEquals("Calle 10 # 5-20", vo.direccion());
    }

    @Test
    void producto_registrar_entregaTodosLosCamposAlService() throws Exception {
        ProductoService service = mock(ProductoService.class);

        mvc(new ProductoController(service))
                .perform(post("/productos/guardar")
                        .param("nombre", "Cera para cabello")
                        .param("descripcion", "Fijación fuerte")
                        .param("precioCompra", "12000")
                        .param("precioVenta", "20000.50")
                        .param("cantidadDisponible", "10")
                        .param("stockMinimo", "3"))
                .andExpect(redirectedUrl("/productos"));

        ArgumentCaptor<ProductoVO> captor = ArgumentCaptor.forClass(ProductoVO.class);
        verify(service).registrar(captor.capture());
        ProductoVO vo = captor.getValue();
        assertEquals("Cera para cabello", vo.nombre());
        assertEquals("Fijación fuerte", vo.descripcion());
        assertEquals(new BigDecimal("12000"), vo.precioCompra());
        assertEquals(new BigDecimal("20000.50"), vo.precioVenta());
        assertEquals(10, vo.cantidadDisponible());
        assertEquals(3, vo.stockMinimo());
    }

    @Test
    void horario_registrar_entregaTodosLosCamposAlService() throws Exception {
        HorarioBarberoService service = mock(HorarioBarberoService.class);
        when(service.registrar(any(HorarioBarberoVO.class)))
                .thenReturn(new HorarioBarberoVO(1L, 4L, 1, LocalTime.of(9, 0), LocalTime.of(17, 30)));

        mvc(new HorarioBarberoController(service, mock(BarberoService.class)))
                .perform(post("/horarios/guardar")
                        .param("idBarbero", "4")
                        .param("diaSemana", "1")
                        .param("horaInicio", "09:00")
                        .param("horaFin", "17:30"))
                .andExpect(redirectedUrl("/horarios/barbero/4"));

        ArgumentCaptor<HorarioBarberoVO> captor = ArgumentCaptor.forClass(HorarioBarberoVO.class);
        verify(service).registrar(captor.capture());
        HorarioBarberoVO vo = captor.getValue();
        assertEquals(4L, vo.idBarbero());
        assertEquals(1, vo.diaSemana());
        assertEquals(LocalTime.of(9, 0), vo.horaInicio());
        assertEquals(LocalTime.of(17, 30), vo.horaFin());
    }

    @Test
    void cita_registrar_entregaTodosLosCamposAlService() throws Exception {
        CitaService service = mock(CitaService.class);
        when(service.registrar(any(CitaVO.class))).thenReturn(new CitaVO(
                55L, 2L, 4L, LocalDateTime.of(2026, 12, 1, 10, 30), LocalDateTime.of(2026, 12, 1, 11, 15),
                EstadoCita.PROGRAMADA, null, null, null));

        mvc(new CitaController(service, mock(ClienteService.class), mock(BarberoService.class),
                        mock(ServicioService.class), mock(CitaServicioService.class)))
                .perform(post("/citas/guardar")
                        .param("idCliente", "2")
                        .param("idBarbero", "4")
                        .param("fechaHoraInicio", "2026-12-01T10:30")   // formato de datetime-local
                        .param("fechaHoraFin", "2026-12-01T11:15")
                        .param("observaciones", "Primera visita"))
                .andExpect(redirectedUrl("/citas/55"));

        ArgumentCaptor<CitaVO> captor = ArgumentCaptor.forClass(CitaVO.class);
        verify(service).registrar(captor.capture());
        CitaVO vo = captor.getValue();
        assertNull(vo.idCita());
        assertEquals(2L, vo.idCliente());
        assertEquals(4L, vo.idBarbero());
        assertEquals(LocalDateTime.of(2026, 12, 1, 10, 30), vo.fechaHoraInicio());
        assertEquals(LocalDateTime.of(2026, 12, 1, 11, 15), vo.fechaHoraFin());
        assertNull(vo.estado());                       // el Service aplica PROGRAMADA por defecto
        assertEquals("Primera visita", vo.observaciones());
    }

    @Test
    void pago_registrar_entregaTodosLosCamposAlService() throws Exception {
        PagoService service = mock(PagoService.class);
        when(service.registrar(any(PagoVO.class))).thenReturn(new PagoVO(
                9L, 7L, new BigDecimal("45000.50"), BigDecimal.ZERO, MetodoPago.TARJETA_DEBITO,
                EstadoPago.PAGADO, "REF-1", LocalDateTime.of(2026, 11, 17, 9, 15), null));

        mvc(new PagoController(service, mock(CitaService.class)))
                .perform(post("/pagos/guardar")
                        .param("idCita", "7")
                        .param("monto", "45000.50")
                        .param("propina", "")
                        .param("metodoPago", "TARJETA_DEBITO")
                        .param("estado", "PAGADO")
                        .param("referencia", "REF-1")
                        .param("fechaPago", "2026-11-17T09:15"))
                .andExpect(redirectedUrl("/pagos/cita/7"));

        ArgumentCaptor<PagoVO> captor = ArgumentCaptor.forClass(PagoVO.class);
        verify(service).registrar(captor.capture());
        PagoVO vo = captor.getValue();
        assertEquals(7L, vo.idCita());
        assertEquals(new BigDecimal("45000.50"), vo.monto());
        assertNull(vo.propina());
        assertEquals(MetodoPago.TARJETA_DEBITO, vo.metodoPago());
        assertEquals(EstadoPago.PAGADO, vo.estado());
        assertEquals("REF-1", vo.referencia());
        assertEquals(LocalDateTime.of(2026, 11, 17, 9, 15), vo.fechaPago());
    }

    @Test
    void movimientoInventario_registrar_entregaTodosLosCamposAlService() throws Exception {
        InventarioService service = mock(InventarioService.class);
        when(service.registrarMovimiento(any(MovimientoInventarioVO.class))).thenReturn(
                new MovimientoInventarioVO(1L, 3L, 5, LocalDateTime.of(2026, 11, 17, 8, 0),
                        TipoMovimientoInventario.ENTRADA));

        mvc(new InventarioController(service, mock(ProductoService.class)))
                .perform(post("/inventario/movimientos/guardar")
                        .param("idProducto", "3")
                        .param("cantidad", "5")
                        .param("fecha", "")
                        .param("tipo", "ENTRADA"))
                .andExpect(redirectedUrl("/inventario"));

        ArgumentCaptor<MovimientoInventarioVO> captor = ArgumentCaptor.forClass(MovimientoInventarioVO.class);
        verify(service).registrarMovimiento(captor.capture());
        MovimientoInventarioVO vo = captor.getValue();
        assertEquals(3L, vo.idProducto());
        assertEquals(5, vo.cantidad());
        assertNull(vo.fecha());
        assertEquals(TipoMovimientoInventario.ENTRADA, vo.tipo());
    }

    @Test
    void actualizar_usaElIdDeLaRutaYNoElDelFormulario() throws Exception {
        BarberoService service = mock(BarberoService.class);

        mvc(new BarberoController(service))
                .perform(post("/barberos/9/actualizar")
                        .param("idBarbero", "999")          // un id manipulado en el formulario se ignora
                        .param("nombres", "Juan")
                        .param("apellidos", "Pérez")
                        .param("documentoIdentidad", "1020304050")
                        .param("estado", "VACACIONES"))
                .andExpect(redirectedUrl("/barberos"));

        ArgumentCaptor<BarberoVO> captor = ArgumentCaptor.forClass(BarberoVO.class);
        verify(service).actualizar(captor.capture());
        assertEquals(9L, captor.getValue().idBarbero());
        assertEquals(EstadoBarbero.VACACIONES, captor.getValue().estado());
    }

    @Test
    void errorDeNegocio_vuelveAlFormularioConMensaje() throws Exception {
        BarberoService service = mock(BarberoService.class);
        when(service.registrar(any(BarberoVO.class)))
                .thenThrow(new ReglaNegocioException("Ya existe un barbero con ese documento"));

        mvc(new BarberoController(service))
                .perform(post("/barberos/guardar")
                        .header("Referer", "http://localhost/barberos/nuevo")
                        .param("nombres", "Juan"))
                .andExpect(redirectedUrl("/barberos/nuevo"))
                .andExpect(flash().attribute("error", "Ya existe un barbero con ese documento"));
    }

    @Test
    void fechaConFormatoInvalido_noLlegaAlServiceYMuestraError() throws Exception {
        BarberoService service = mock(BarberoService.class);

        mvc(new BarberoController(service))
                .perform(post("/barberos/guardar")
                        .param("nombres", "Juan")
                        .param("fechaContratacion", "15/01/2026"))
                .andExpect(redirectedUrl("/barberos"))
                .andExpect(flash().attributeExists("error"));

        verifyNoInteractions(service);
    }
}
