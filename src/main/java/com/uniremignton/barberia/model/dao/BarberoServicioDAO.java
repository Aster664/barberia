package com.uniremignton.barberia.model.dao;

import com.uniremignton.barberia.model.vo.BarberoServicioVO;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BarberoServicioDAO {

    private final JdbcTemplate jdbcTemplate;

    public BarberoServicioDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void asociar(BarberoServicioVO relacion) {
        Objects.requireNonNull(relacion, "La relación barbero-servicio es obligatoria");
        jdbcTemplate.update(
                "INSERT INTO barbero_servicio (id_barbero, id_servicio) VALUES (?, ?)",
                relacion.idBarbero(), relacion.idServicio());
    }

    public boolean desasociar(long idBarbero, long idServicio) {
        return jdbcTemplate.update(
                "DELETE FROM barbero_servicio WHERE id_barbero = ? AND id_servicio = ?",
                idBarbero,
                idServicio) > 0;
    }

    public List<BarberoServicioVO> listarPorBarbero(long idBarbero) {
        return jdbcTemplate.query(
                """
                SELECT id_barbero, id_servicio
                FROM barbero_servicio
                WHERE id_barbero = ?
                ORDER BY id_servicio
                """,
                (rs, rowNum) -> new BarberoServicioVO(
                        rs.getLong("id_barbero"),
                        rs.getLong("id_servicio")),
                idBarbero);
    }

    public List<BarberoServicioVO> listarPorServicio(long idServicio) {
        return jdbcTemplate.query(
                """
                SELECT id_barbero, id_servicio
                FROM barbero_servicio
                WHERE id_servicio = ?
                ORDER BY id_barbero
                """,
                (rs, rowNum) -> new BarberoServicioVO(
                        rs.getLong("id_barbero"),
                        rs.getLong("id_servicio")),
                idServicio);
    }
}
