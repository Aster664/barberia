package com.uniremignton.barberia.model.vo;

/** Value object de la relación muchos a muchos entre barberos y servicios. */
public record BarberoServicioVO(Long idBarbero, Long idServicio) {

    public BarberoServicioVO() {
        this(null, null);
    }
}