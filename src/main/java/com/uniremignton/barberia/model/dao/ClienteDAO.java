package com.uniremignton.barberia.model.dao;

import com.uniremignton.barberia.model.vo.ClienteVO;
import com.uniremignton.barberia.model.vo.EstadoCliente;
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
public class ClienteDAO {

    private static final RowMapper<ClienteVO> ROW_MAPPER = (rs, rowNum) -> new ClienteVO(
            rs.getLong("id_cliente"),
            rs.getString("nombres"),
            rs.getString("apellidos"),
            rs.getString("telefono"),
            rs.getString("email"),
            rs.getDate("fecha_nacimiento") == null ? null : rs.getDate("fecha_nacimiento").toLocalDate(),
            rs.getString("notas"),
            EstadoCliente.valueOf(rs.getString("estado")),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime());

    private final JdbcTemplate jdbcTemplate;

    public ClienteDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ClienteVO crear(ClienteVO cliente) {
        Objects.requireNonNull(cliente, "El cliente es obligatorio");
        Objects.requireNonNull(cliente.estado(), "El estado del cliente es obligatorio");
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO clientes
                        (nombres, apellidos, telefono, email, fecha_nacimiento, notas, estado)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, cliente.nombres());
            statement.setString(2, cliente.apellidos());
            statement.setString(3, cliente.telefono());
            statement.setString(4, cliente.email());
            statement.setObject(5, cliente.fechaNacimiento());
            statement.setString(6, cliente.notas());
            statement.setString(7, cliente.estado().name());
            return statement;
        }, keyHolder);
        return buscarPorId(generatedId(keyHolder));
    }

    public boolean actualizar(ClienteVO cliente) {
        Objects.requireNonNull(cliente, "El cliente es obligatorio");
        Objects.requireNonNull(cliente.idCliente(), "El id del cliente es obligatorio");
        Objects.requireNonNull(cliente.estado(), "El estado del cliente es obligatorio");
        return jdbcTemplate.update(
                """
                UPDATE clientes
                SET nombres = ?, apellidos = ?, telefono = ?, email = ?,
                    fecha_nacimiento = ?, notas = ?, estado = ?
                WHERE id_cliente = ?
                """,
                cliente.nombres(), cliente.apellidos(), cliente.telefono(), cliente.email(),
                cliente.fechaNacimiento(), cliente.notas(), cliente.estado().name(),
                cliente.idCliente()) > 0;
    }

    public ClienteVO buscarPorId(long idCliente) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM clientes WHERE id_cliente = ?",
                ROW_MAPPER,
                idCliente);
    }

    public List<ClienteVO> listar() {
        return jdbcTemplate.query("SELECT * FROM clientes ORDER BY apellidos, nombres", ROW_MAPPER);
    }

    public boolean eliminar(long idCliente) {
        return jdbcTemplate.update("DELETE FROM clientes WHERE id_cliente = ?", idCliente) > 0;
    }

    private static long generatedId(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("MySQL no devolvió el id del cliente insertado");
        }
        return key.longValue();
    }
}
