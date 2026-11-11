package com.uniremignton.barberia.model.dao;

import com.uniremignton.barberia.model.vo.MovimientoInventarioVO;
import com.uniremignton.barberia.model.vo.TipoMovimientoInventario;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MovimientoInventarioDAO {

    private static final RowMapper<MovimientoInventarioVO> ROW_MAPPER = (rs, rowNum) ->
            new MovimientoInventarioVO(
                    rs.getLong("id_movimiento"),
                    rs.getLong("id_producto"),
                    rs.getInt("cantidad"),
                    rs.getTimestamp("fecha").toLocalDateTime(),
                    TipoMovimientoInventario.valueOf(rs.getString("tipo")));

    private final JdbcTemplate jdbcTemplate;

    public MovimientoInventarioDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public MovimientoInventarioVO registrar(MovimientoInventarioVO movimiento) {
        Objects.requireNonNull(movimiento, "El movimiento es obligatorio");
        Objects.requireNonNull(movimiento.idProducto(), "El producto es obligatorio");
        Objects.requireNonNull(movimiento.cantidad(), "La cantidad es obligatoria");
        Objects.requireNonNull(movimiento.tipo(), "El tipo de movimiento es obligatorio");
        if (movimiento.cantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad del movimiento debe ser mayor que cero");
        }

        int filasActualizadas;
        if (movimiento.tipo() == TipoMovimientoInventario.ENTRADA) {
            filasActualizadas = jdbcTemplate.update(
                    """
                    UPDATE productos
                    SET cantidad_disponible = cantidad_disponible + ?
                    WHERE id_producto = ?
                    """,
                    movimiento.cantidad(),
                    movimiento.idProducto());
        } else {
            filasActualizadas = jdbcTemplate.update(
                    """
                    UPDATE productos
                    SET cantidad_disponible = cantidad_disponible - ?
                    WHERE id_producto = ? AND cantidad_disponible >= ?
                    """,
                    movimiento.cantidad(),
                    movimiento.idProducto(),
                    movimiento.cantidad());
        }

        if (filasActualizadas == 0) {
            boolean existeProducto = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                    "SELECT EXISTS(SELECT 1 FROM productos WHERE id_producto = ?)",
                    Boolean.class,
                    movimiento.idProducto()));
            if (!existeProducto) {
                throw new IllegalArgumentException(
                        "No existe el producto con id " + movimiento.idProducto());
            }
            throw new IllegalStateException("El movimiento dejaría el inventario con stock negativo");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO movimientos_inventario (id_producto, cantidad, fecha, tipo)
                    VALUES (?, ?, COALESCE(?, CURRENT_TIMESTAMP), ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, movimiento.idProducto());
            statement.setInt(2, movimiento.cantidad());
            if (movimiento.fecha() == null) {
                statement.setTimestamp(3, null);
            } else {
                statement.setTimestamp(3, Timestamp.valueOf(movimiento.fecha()));
            }
            statement.setString(4, movimiento.tipo().name());
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("MySQL no devolvió el id del movimiento insertado");
        }
        return buscarPorId(key.longValue());
    }

    public MovimientoInventarioVO buscarPorId(long idMovimiento) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM movimientos_inventario WHERE id_movimiento = ?",
                ROW_MAPPER,
                idMovimiento);
    }

    public List<MovimientoInventarioVO> listarPorProducto(long idProducto) {
        return jdbcTemplate.query(
                """
                SELECT * FROM movimientos_inventario
                WHERE id_producto = ?
                ORDER BY fecha DESC, id_movimiento DESC
                """,
                ROW_MAPPER,
                idProducto);
    }

    public List<MovimientoInventarioVO> listar() {
        return jdbcTemplate.query(
                "SELECT * FROM movimientos_inventario ORDER BY fecha DESC, id_movimiento DESC",
                ROW_MAPPER);
    }
}
