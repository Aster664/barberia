package com.uniremignton.barberia.model.dao;

import com.uniremignton.barberia.model.vo.HorarioBarberoVO;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Time;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class HorarioBarberoDAO {

    private static final RowMapper<HorarioBarberoVO> ROW_MAPPER = (rs, rowNum) -> new HorarioBarberoVO(
            rs.getLong("id_horario"),
            rs.getLong("id_barbero"),
            rs.getInt("dia_semana"),
            rs.getTime("hora_inicio").toLocalTime(),
            rs.getTime("hora_fin").toLocalTime());

    private final JdbcTemplate jdbcTemplate;

    public HorarioBarberoDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public HorarioBarberoVO crear(HorarioBarberoVO horario) {
        validarCampos(horario);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO horarios_barbero (id_barbero, dia_semana, hora_inicio, hora_fin)
                    VALUES (?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, horario.idBarbero());
            statement.setInt(2, horario.diaSemana());
            statement.setTime(3, Time.valueOf(horario.horaInicio()));
            statement.setTime(4, Time.valueOf(horario.horaFin()));
            return statement;
        }, keyHolder);
        return buscarPorId(generatedId(keyHolder));
    }

    public boolean actualizar(HorarioBarberoVO horario) {
        validarCampos(horario);
        Objects.requireNonNull(horario.idHorario(), "El id del horario es obligatorio");
        return jdbcTemplate.update(
                """
                UPDATE horarios_barbero
                SET id_barbero = ?, dia_semana = ?, hora_inicio = ?, hora_fin = ?
                WHERE id_horario = ?
                """,
                horario.idBarbero(), horario.diaSemana(),
                Time.valueOf(horario.horaInicio()), Time.valueOf(horario.horaFin()),
                horario.idHorario()) > 0;
    }

    public HorarioBarberoVO buscarPorId(long idHorario) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM horarios_barbero WHERE id_horario = ?",
                ROW_MAPPER,
                idHorario);
    }

    public List<HorarioBarberoVO> listar() {
        return jdbcTemplate.query(
                "SELECT * FROM horarios_barbero ORDER BY id_barbero, dia_semana, hora_inicio",
                ROW_MAPPER);
    }

    public List<HorarioBarberoVO> listarPorBarbero(long idBarbero) {
        return jdbcTemplate.query(
                """
                SELECT * FROM horarios_barbero
                WHERE id_barbero = ?
                ORDER BY dia_semana, hora_inicio
                """,
                ROW_MAPPER,
                idBarbero);
    }

    public boolean eliminar(long idHorario) {
        return jdbcTemplate.update("DELETE FROM horarios_barbero WHERE id_horario = ?", idHorario) > 0;
    }

    private static void validarCampos(HorarioBarberoVO horario) {
        Objects.requireNonNull(horario, "El horario es obligatorio");
        Objects.requireNonNull(horario.idBarbero(), "El barbero del horario es obligatorio");
        Objects.requireNonNull(horario.diaSemana(), "El día de la semana es obligatorio");
        Objects.requireNonNull(horario.horaInicio(), "La hora de inicio es obligatoria");
        Objects.requireNonNull(horario.horaFin(), "La hora de fin es obligatoria");
    }

    private static long generatedId(KeyHolder keyHolder) {
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("MySQL no devolvió el id del horario insertado");
        }
        return key.longValue();
    }
}
