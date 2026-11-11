package com.uniremignton.barberia.model.vo;

/** Value object inmutable que representa un proveedor. */
public record ProveedorVO(
        Long idProveedor,
        String nombre,
        String telefono,
        String correo,
        String direccion) {

    public ProveedorVO() {
        this(null, null, null, null, null);
    }
}
