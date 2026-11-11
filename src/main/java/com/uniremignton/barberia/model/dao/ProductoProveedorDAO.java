package com.uniremignton.barberia.model.dao;

import com.uniremignton.barberia.model.vo.ProductoProveedorVO;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ProductoProveedorDAO {

    private final JdbcTemplate jdbcTemplate;

    public ProductoProveedorDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void asociar(ProductoProveedorVO relacion) {
        Objects.requireNonNull(relacion, "La relación producto-proveedor es obligatoria");
        jdbcTemplate.update(
                "INSERT INTO productos_proveedores (id_producto, id_proveedor) VALUES (?, ?)",
                relacion.idProducto(), relacion.idProveedor());
    }

    public boolean desasociar(long idProducto, long idProveedor) {
        return jdbcTemplate.update(
                "DELETE FROM productos_proveedores WHERE id_producto = ? AND id_proveedor = ?",
                idProducto,
                idProveedor) > 0;
    }

    public List<ProductoProveedorVO> listarPorProducto(long idProducto) {
        return jdbcTemplate.query(
                """
                SELECT id_producto, id_proveedor
                FROM productos_proveedores
                WHERE id_producto = ?
                ORDER BY id_proveedor
                """,
                (rs, rowNum) -> new ProductoProveedorVO(
                        rs.getLong("id_producto"),
                        rs.getLong("id_proveedor")),
                idProducto);
    }

    public List<ProductoProveedorVO> listarPorProveedor(long idProveedor) {
        return jdbcTemplate.query(
                """
                SELECT id_producto, id_proveedor
                FROM productos_proveedores
                WHERE id_proveedor = ?
                ORDER BY id_producto
                """,
                (rs, rowNum) -> new ProductoProveedorVO(
                        rs.getLong("id_producto"),
                        rs.getLong("id_proveedor")),
                idProveedor);
    }
}
