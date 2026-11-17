package com.uniremignton.barberia.model.dao;

import com.uniremignton.barberia.model.vo.EstadoPago;
import com.uniremignton.barberia.model.vo.MetodoPago;
import com.uniremignton.barberia.model.vo.PagoVO;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PagoDAO {

    private static final RowMapper<PagoVO> ROW_MAPPER = (rs, rowNum) -> new PagoVO(
            rs.getLong("id_pago"),
            rs.getLong("id_cita"),
            rs.getBigDecimal("monto"),
            rs.getBigDecimal("propina"),
            MetodoPago.valueOf(rs.getString("metodo_pago")),
            EstadoPago.valueOf(rs.getString("estado")),
            rs.getString("referencia"),
            rs.getTimestamp("fecha_pago") == null ? null : rs.getTimestamp("fecha_pago").toLocalDateTime(),
            rs.getTimestamp("created_at").toLocalDateTime());

    private final JdbcTemplate jdbcTemplate;

    public PagoDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Crea el pago; una propina nula se guarda como 0 (la columna es NOT NULL). */
    public PagoVO crear(PagoVO pago) {
        validarCampos(pago);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO pagos
                        (id_cita, monto, propina, metodo_pago, estado, referencia, fecha_pago)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, pago.idCita());
            statement.setBigDecimal(2, pago.monto());
            statement.setBigDecimal(3, propinaONulo(pago));
            statement.setString(4, pago.metodoPago().name());
            statement.setString(5, pago.estado().name());
            statement.setString(6, pago.referencia());
            statement.setTimestamp(7, aTimestamp(pago.fechaPago()));
            return statement;
        }, keyHolder);
        return buscarPorId(generatedId(keyHolder));
    }

    public boolean actualizar(PagoVO pago) {
        validarCampos(pago);
        Objects.requireNonNull(pago.idPago(), "El id del pago es obligatorio");
        return jdbcTemplate.update(
                """
                UPDATE pagos
                SET id_cita = ?, monto = ?, propina = ?, metodo_pago = ?, estado = ?,
                    referencia = ?, fecha_pago = ?
                WHERE id_pago = ?
                """,
                pago.idCita(), pago.monto(), propinaONulo(pago), pago.metodoPago().name(),
                pago.estado().name(), pago.referencia(), aTimestamp(pago.fechaPago()),
                pago.idPago()) > 0;
    }

    public PagoVO buscarPorId(long idPago) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM pagos WHERE id_pago = ?",
                ROW_MAPPER,
                idPago);
    }

    public List<PagoVO> listar() {
        return jdbcTemplate.query("SELECT * FROM pagos ORDER BY created_at DESC, id_pago DESC", ROW_MAPPER);
    }

    public List<PagoVO> listarPorCita(long idCita) {
        return jdbcTemplate.query(
                "SELECT * FROM pagos WHERE id_cita = ? ORDER BY created_at, id_pago",
                ROW_MAPPER,
                idCita);
    }

    public boolean eliminar(long idPago) {
        return jdbcTemplate.update("DELETE FROM pagos WHERE id_pago = ?", idPago) > 0;
    }

    private static void validarCampos(PagoVO pago) {
        Objects.requireNonNull(pago, "El pago es obligatorio");
        Objects.requireNonNull(pago.idCita(), "La cita del pago es obligatoria");
        Objects.requireNonNull(pago.monto(), "El monto del pago es obligatorio");
        Objects.requireNonNull(pago.metodoPago(), "El método de pago es obligatorio");
        Objects.requireNonNull(pago.estado(), "El estado del pago es obligatorio");
    }

    private static BigDecimal propinaONulo(PagoVO pago) {
        return pago.propina() == null ? BigDecimal.ZERO : pago.propina();
    }

    private static Timestamp aTimestamp(LocalDateTime fecha) {
        return fecha == null ? null : Timestamp.valueOf(fecha);
    }

    private static long generatedId(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("MySQL no devolvió el id del pago insertado");
        }
        return key.longValue();
    }
}
