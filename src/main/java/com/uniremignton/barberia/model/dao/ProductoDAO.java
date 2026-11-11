package com.uniremignton.barberia.model.dao;

import com.uniremignton.barberia.model.vo.ProductoVO;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ProductoDAO {

    private static final RowMapper<ProductoVO> ROW_MAPPER = (rs, rowNum) -> new ProductoVO(
            rs.getLong("id_producto"),
            rs.getString("nombre"),
            rs.getString("descripcion"),
            rs.getBigDecimal("precio_compra"),
            rs.getBigDecimal("precio_venta"),
            rs.getInt("cantidad_disponible"),
            rs.getInt("stock_minimo"));

    private final JdbcTemplate jdbcTemplate;

    public ProductoDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public ProductoVO crear(ProductoVO producto) {
        Objects.requireNonNull(producto, "El producto es obligatorio");
        int stockInicial = producto.cantidadDisponible() == null ? 0 : producto.cantidadDisponible();
        if (stockInicial < 0) {
            throw new IllegalArgumentException("La cantidad inicial no puede ser negativa");
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO productos
                        (nombre, descripcion, precio_compra, precio_venta, cantidad_disponible, stock_minimo)
                    VALUES (?, ?, ?, ?, 0, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, producto.nombre());
            statement.setString(2, producto.descripcion());
            statement.setBigDecimal(3, producto.precioCompra());
            statement.setBigDecimal(4, producto.precioVenta());
            statement.setObject(5, producto.stockMinimo() == null ? 0 : producto.stockMinimo());
            return statement;
        }, keyHolder);

        long idProducto = generatedId(keyHolder);
        if (stockInicial > 0) {
            jdbcTemplate.update(
                    """
                    INSERT INTO movimientos_inventario (id_producto, cantidad, tipo)
                    VALUES (?, ?, 'ENTRADA')
                    """,
                    idProducto,
                    stockInicial);
            jdbcTemplate.update(
                    "UPDATE productos SET cantidad_disponible = ? WHERE id_producto = ?",
                    stockInicial,
                    idProducto);
        }
        return buscarPorId(idProducto);
    }

    public boolean actualizar(ProductoVO producto) {
        Objects.requireNonNull(producto, "El producto es obligatorio");
        Objects.requireNonNull(producto.idProducto(), "El id del producto es obligatorio");
        return jdbcTemplate.update(
                """
                UPDATE productos
                SET nombre = ?, descripcion = ?, precio_compra = ?, precio_venta = ?, stock_minimo = ?
                WHERE id_producto = ?
                """,
                producto.nombre(), producto.descripcion(), producto.precioCompra(),
                producto.precioVenta(), producto.stockMinimo(), producto.idProducto()) > 0;
    }

    public ProductoVO buscarPorId(long idProducto) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM productos WHERE id_producto = ?",
                ROW_MAPPER,
                idProducto);
    }

    public List<ProductoVO> listar() {
        return jdbcTemplate.query("SELECT * FROM productos ORDER BY nombre, id_producto", ROW_MAPPER);
    }

    public boolean eliminar(long idProducto) {
        return jdbcTemplate.update("DELETE FROM productos WHERE id_producto = ?", idProducto) > 0;
    }

    private static long generatedId(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("MySQL no devolvió el id del producto insertado");
        }
        return key.longValue();
    }
}
