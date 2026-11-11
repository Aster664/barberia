package com.uniremignton.barberia.model.dao;

import com.uniremignton.barberia.model.vo.ProveedorVO;
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
public class ProveedorDAO {

    private static final RowMapper<ProveedorVO> ROW_MAPPER = (rs, rowNum) -> new ProveedorVO(
            rs.getLong("id_proveedor"),
            rs.getString("nombre"),
            rs.getString("telefono"),
            rs.getString("correo"),
            rs.getString("direccion"));

    private final JdbcTemplate jdbcTemplate;

    public ProveedorDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ProveedorVO crear(ProveedorVO proveedor) {
        Objects.requireNonNull(proveedor, "El proveedor es obligatorio");
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO proveedores (nombre, telefono, correo, direccion) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, proveedor.nombre());
            statement.setString(2, proveedor.telefono());
            statement.setString(3, proveedor.correo());
            statement.setString(4, proveedor.direccion());
            return statement;
        }, keyHolder);
        return buscarPorId(generatedId(keyHolder));
    }

    public boolean actualizar(ProveedorVO proveedor) {
        Objects.requireNonNull(proveedor, "El proveedor es obligatorio");
        Objects.requireNonNull(proveedor.idProveedor(), "El id del proveedor es obligatorio");
        return jdbcTemplate.update(
                """
                UPDATE proveedores
                SET nombre = ?, telefono = ?, correo = ?, direccion = ?
                WHERE id_proveedor = ?
                """,
                proveedor.nombre(), proveedor.telefono(), proveedor.correo(),
                proveedor.direccion(), proveedor.idProveedor()) > 0;
    }

    public ProveedorVO buscarPorId(long idProveedor) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM proveedores WHERE id_proveedor = ?",
                ROW_MAPPER,
                idProveedor);
    }

    public List<ProveedorVO> listar() {
        return jdbcTemplate.query("SELECT * FROM proveedores ORDER BY nombre, id_proveedor", ROW_MAPPER);
    }

    public boolean eliminar(long idProveedor) {
        return jdbcTemplate.update("DELETE FROM proveedores WHERE id_proveedor = ?", idProveedor) > 0;
    }

    private static long generatedId(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("MySQL no devolvió el id del proveedor insertado");
        }
        return key.longValue();
    }
}
