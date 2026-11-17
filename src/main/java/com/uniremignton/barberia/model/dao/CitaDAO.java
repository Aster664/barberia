package com.uniremignton.barberia.model.dao;

import com.uniremignton.barberia.model.vo.CitaVO;
import com.uniremignton.barberia.model.vo.EstadoCita;
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
public class CitaDAO {

    private static final RowMapper<CitaVO> ROW_MAPPER = (rs, rowNum) -> new CitaVO(
            rs.getLong("id_cita"),
            rs.getLong("id_cliente"),
            rs.getLong("id_barbero"),
            rs.getTimestamp("fecha_hora_inicio").toLocalDateTime(),
            rs.getTimestamp("fecha_hora_fin").toLocalDateTime(),
            EstadoCita.valueOf(rs.getString("estado")),
            rs.getString("observaciones"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime());

    private final JdbcTemplate jdbcTemplate;

    public CitaDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CitaVO crear(CitaVO cita) {
        validarCampos(cita);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO citas
                        (id_cliente, id_barbero, fecha_hora_inicio, fecha_hora_fin, estado, observaciones)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, cita.idCliente());
            statement.setLong(2, cita.idBarbero());
            statement.setTimestamp(3, Timestamp.valueOf(cita.fechaHoraInicio()));
            statement.setTimestamp(4, Timestamp.valueOf(cita.fechaHoraFin()));
            statement.setString(5, cita.estado().name());
            statement.setString(6, cita.observaciones());
            return statement;
        }, keyHolder);
        return buscarPorId(generatedId(keyHolder));
    }

    public boolean actualizar(CitaVO cita) {
        validarCampos(cita);
        Objects.requireNonNull(cita.idCita(), "El id de la cita es obligatorio");
        return jdbcTemplate.update(
                """
                UPDATE citas
                SET id_cliente = ?, id_barbero = ?, fecha_hora_inicio = ?, fecha_hora_fin = ?,
                    estado = ?, observaciones = ?
                WHERE id_cita = ?
                """,
                cita.idCliente(), cita.idBarbero(),
                Timestamp.valueOf(cita.fechaHoraInicio()), Timestamp.valueOf(cita.fechaHoraFin()),
                cita.estado().name(), cita.observaciones(), cita.idCita()) > 0;
    }

    public CitaVO buscarPorId(long idCita) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM citas WHERE id_cita = ?",
                ROW_MAPPER,
                idCita);
    }

    public List<CitaVO> listar() {
        return jdbcTemplate.query(
                "SELECT * FROM citas ORDER BY fecha_hora_inicio DESC, id_cita DESC",
                ROW_MAPPER);
    }

    public List<CitaVO> listarPorCliente(long idCliente) {
        return jdbcTemplate.query(
                """
                SELECT * FROM citas
                WHERE id_cliente = ?
                ORDER BY fecha_hora_inicio DESC, id_cita DESC
                """,
                ROW_MAPPER,
                idCliente);
    }

    public List<CitaVO> listarPorBarbero(long idBarbero) {
        return jdbcTemplate.query(
                """
                SELECT * FROM citas
                WHERE id_barbero = ?
                ORDER BY fecha_hora_inicio DESC, id_cita DESC
                """,
                ROW_MAPPER,
                idBarbero);
    }

    /** Citas cuyo inicio está en el rango [desde, hasta), ordenadas cronológicamente. */
    public List<CitaVO> listarPorRango(LocalDateTime desde, LocalDateTime hasta) {
        Objects.requireNonNull(desde, "La fecha inicial es obligatoria");
        Objects.requireNonNull(hasta, "La fecha final es obligatoria");
        return jdbcTemplate.query(
                """
                SELECT * FROM citas
                WHERE fecha_hora_inicio >= ? AND fecha_hora_inicio < ?
                ORDER BY fecha_hora_inicio, id_cita
                """,
                ROW_MAPPER,
                Timestamp.valueOf(desde),
                Timestamp.valueOf(hasta));
    }

    /**
     * Indica si el barbero ya tiene una cita vigente (no CANCELADA ni NO_ASISTIO) que se cruza con
     * el intervalo dado. {@code idCitaExcluida} permite ignorar la propia cita al reprogramarla.
     */
    public boolean existeSolapamiento(
            long idBarbero, LocalDateTime inicio, LocalDateTime fin, Long idCitaExcluida) {
        Objects.requireNonNull(inicio, "La fecha de inicio es obligatoria");
        Objects.requireNonNull(fin, "La fecha de fin es obligatoria");
        long excluida = idCitaExcluida == null ? 0L : idCitaExcluida;
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                """
                SELECT EXISTS(
                    SELECT 1 FROM citas
                    WHERE id_barbero = ?
                      AND estado NOT IN ('CANCELADA', 'NO_ASISTIO')
                      AND fecha_hora_inicio < ?
                      AND fecha_hora_fin > ?
                      AND id_cita <> ?)
                """,
                Boolean.class,
                idBarbero,
                Timestamp.valueOf(fin),
                Timestamp.valueOf(inicio),
                excluida));
    }

    public boolean eliminar(long idCita) {
        return jdbcTemplate.update("DELETE FROM citas WHERE id_cita = ?", idCita) > 0;
    }

    private static void validarCampos(CitaVO cita) {
        Objects.requireNonNull(cita, "La cita es obligatoria");
        Objects.requireNonNull(cita.idCliente(), "El cliente de la cita es obligatorio");
        Objects.requireNonNull(cita.idBarbero(), "El barbero de la cita es obligatorio");
        Objects.requireNonNull(cita.fechaHoraInicio(), "La fecha y hora de inicio son obligatorias");
        Objects.requireNonNull(cita.fechaHoraFin(), "La fecha y hora de fin son obligatorias");
        Objects.requireNonNull(cita.estado(), "El estado de la cita es obligatorio");
    }

    private static long generatedId(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("MySQL no devolvió el id de la cita insertada");
        }
        return key.longValue();
    }
}
