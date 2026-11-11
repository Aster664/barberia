package com.uniremignton.barberia.model.dao;

import com.uniremignton.barberia.model.vo.BarberoVO;
import com.uniremignton.barberia.model.vo.EstadoBarbero;
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
public class BarberoDAO {

    private static final RowMapper<BarberoVO> ROW_MAPPER = (rs, rowNum) -> new BarberoVO(
            rs.getLong("id_barbero"),
            rs.getString("nombres"),
            rs.getString("apellidos"),
            rs.getString("documento_identidad"),
            rs.getString("telefono"),
            rs.getString("email"),
            rs.getDate("fecha_contratacion") == null ? null : rs.getDate("fecha_contratacion").toLocalDate(),
            rs.getBigDecimal("porcentaje_comision"),
            EstadoBarbero.valueOf(rs.getString("estado")),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime());

    private final JdbcTemplate jdbcTemplate;

    public BarberoDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BarberoVO crear(BarberoVO barbero) {
        Objects.requireNonNull(barbero, "El barbero es obligatorio");
        Objects.requireNonNull(barbero.estado(), "El estado del barbero es obligatorio");
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO barberos
                        (nombres, apellidos, documento_identidad, telefono, email,
                         fecha_contratacion, porcentaje_comision, estado)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, barbero.nombres());
            statement.setString(2, barbero.apellidos());
            statement.setString(3, barbero.documentoIdentidad());
            statement.setString(4, barbero.telefono());
            statement.setString(5, barbero.email());
            statement.setObject(6, barbero.fechaContratacion());
            statement.setBigDecimal(7, barbero.porcentajeComision());
            statement.setString(8, barbero.estado().name());
            return statement;
        }, keyHolder);
        return buscarPorId(generatedId(keyHolder));
    }

    public boolean actualizar(BarberoVO barbero) {
        Objects.requireNonNull(barbero, "El barbero es obligatorio");
        Objects.requireNonNull(barbero.idBarbero(), "El id del barbero es obligatorio");
        Objects.requireNonNull(barbero.estado(), "El estado del barbero es obligatorio");
        return jdbcTemplate.update(
                """
                UPDATE barberos
                SET nombres = ?, apellidos = ?, documento_identidad = ?, telefono = ?, email = ?,
                    fecha_contratacion = ?, porcentaje_comision = ?, estado = ?
                WHERE id_barbero = ?
                """,
                barbero.nombres(), barbero.apellidos(), barbero.documentoIdentidad(),
                barbero.telefono(), barbero.email(), barbero.fechaContratacion(),
                barbero.porcentajeComision(), barbero.estado().name(), barbero.idBarbero()) > 0;
    }

    public BarberoVO buscarPorId(long idBarbero) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM barberos WHERE id_barbero = ?",
                ROW_MAPPER,
                idBarbero);
    }

    public List<BarberoVO> listar() {
        return jdbcTemplate.query("SELECT * FROM barberos ORDER BY apellidos, nombres", ROW_MAPPER);
    }

    public boolean eliminar(long idBarbero) {
        return jdbcTemplate.update("DELETE FROM barberos WHERE id_barbero = ?", idBarbero) > 0;
    }

    private static long generatedId(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("MySQL no devolvió el id del barbero insertado");
        }
        return key.longValue();
    }
}
