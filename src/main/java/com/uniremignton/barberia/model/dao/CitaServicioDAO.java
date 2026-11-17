package com.uniremignton.barberia.model.dao;

import com.uniremignton.barberia.model.vo.CitaServicioVO;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class CitaServicioDAO {

    private static final RowMapper<CitaServicioVO> ROW_MAPPER = (rs, rowNum) -> new CitaServicioVO(
            rs.getLong("id_cita"),
            rs.getLong("id_servicio"),
            rs.getInt("cantidad"),
            rs.getBigDecimal("precio_aplicado"));

    private final JdbcTemplate jdbcTemplate;

    public CitaServicioDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void asociar(CitaServicioVO detalle) {
        validarCampos(detalle);
        jdbcTemplate.update(
                """
                INSERT INTO cita_servicio (id_cita, id_servicio, cantidad, precio_aplicado)
                VALUES (?, ?, ?, ?)
                """,
                detalle.idCita(), detalle.idServicio(), detalle.cantidad(), detalle.precioAplicado());
    }

    public boolean actualizar(CitaServicioVO detalle) {
        validarCampos(detalle);
        return jdbcTemplate.update(
                """
                UPDATE cita_servicio
                SET cantidad = ?, precio_aplicado = ?
                WHERE id_cita = ? AND id_servicio = ?
                """,
                detalle.cantidad(), detalle.precioAplicado(),
                detalle.idCita(), detalle.idServicio()) > 0;
    }

    public boolean desasociar(long idCita, long idServicio) {
        return jdbcTemplate.update(
                "DELETE FROM cita_servicio WHERE id_cita = ? AND id_servicio = ?",
                idCita,
                idServicio) > 0;
    }

    public CitaServicioVO buscarPorId(long idCita, long idServicio) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM cita_servicio WHERE id_cita = ? AND id_servicio = ?",
                ROW_MAPPER,
                idCita,
                idServicio);
    }

    public List<CitaServicioVO> listarPorCita(long idCita) {
        return jdbcTemplate.query(
                "SELECT * FROM cita_servicio WHERE id_cita = ? ORDER BY id_servicio",
                ROW_MAPPER,
                idCita);
    }

    public List<CitaServicioVO> listarPorServicio(long idServicio) {
        return jdbcTemplate.query(
                "SELECT * FROM cita_servicio WHERE id_servicio = ? ORDER BY id_cita",
                ROW_MAPPER,
                idServicio);
    }

    private static void validarCampos(CitaServicioVO detalle) {
        Objects.requireNonNull(detalle, "El detalle cita-servicio es obligatorio");
        Objects.requireNonNull(detalle.idCita(), "La cita es obligatoria");
        Objects.requireNonNull(detalle.idServicio(), "El servicio es obligatorio");
        Objects.requireNonNull(detalle.cantidad(), "La cantidad es obligatoria");
        Objects.requireNonNull(detalle.precioAplicado(), "El precio aplicado es obligatorio");
    }
}
