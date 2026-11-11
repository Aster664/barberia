package com.uniremignton.barberia.model.dao;

import com.uniremignton.barberia.model.vo.EstadoServicio;
import com.uniremignton.barberia.model.vo.ServicioVO;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ServicioDAO {

    private static final RowMapper<ServicioVO> ROW_MAPPER = (rs, rowNum) -> new ServicioVO(
            rs.getLong("id_servicio"),
            rs.getString("nombre"),
            rs.getString("descripcion"),
            rs.getInt("duracion_minutos"),
            rs.getBigDecimal("precio"),
            EstadoServicio.valueOf(rs.getString("estado")),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime());

    private final JdbcTemplate jdbcTemplate;

    public ServicioDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ServicioVO crear(ServicioVO servicio) {
        Objects.requireNonNull(servicio, "El servicio es obligatorio");
        Objects.requireNonNull(servicio.estado(), "El estado del servicio es obligatorio");
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO servicios (nombre, descripcion, duracion_minutos, precio, estado)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, servicio.nombre());
            statement.setString(2, servicio.descripcion());
            statement.setObject(3, servicio.duracionMinutos());
            statement.setBigDecimal(4, servicio.precio());
            statement.setString(5, servicio.estado().name());
            return statement;
        }, keyHolder);
        return buscarPorId(generatedId(keyHolder));
    }

    public boolean actualizar(ServicioVO servicio) {
        Objects.requireNonNull(servicio, "El servicio es obligatorio");
        Objects.requireNonNull(servicio.idServicio(), "El id del servicio es obligatorio");
        Objects.requireNonNull(servicio.estado(), "El estado del servicio es obligatorio");
        return jdbcTemplate.update(
                """
                UPDATE servicios
                SET nombre = ?, descripcion = ?, duracion_minutos = ?, precio = ?, estado = ?
                WHERE id_servicio = ?
                """,
                servicio.nombre(), servicio.descripcion(), servicio.duracionMinutos(),
                servicio.precio(), servicio.estado().name(), servicio.idServicio()) > 0;
    }

    public ServicioVO buscarPorId(long idServicio) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM servicios WHERE id_servicio = ?",
                ROW_MAPPER,
                idServicio);
    }

    public List<ServicioVO> listar() {
        return jdbcTemplate.query("SELECT * FROM servicios ORDER BY nombre, id_servicio", ROW_MAPPER);
    }

    public boolean eliminar(long idServicio) {
        return jdbcTemplate.update("DELETE FROM servicios WHERE id_servicio = ?", idServicio) > 0;
    }

    private static long generatedId(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("MySQL no devolvió el id del servicio insertado");
        }
        return key.longValue();
    }
}
