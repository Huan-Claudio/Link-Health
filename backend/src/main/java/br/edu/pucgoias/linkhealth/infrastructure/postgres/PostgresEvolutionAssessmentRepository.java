package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.*;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.*;

@Repository
@Profile("postgres")
public class PostgresEvolutionAssessmentRepository implements EvolutionAssessmentRepository {
    private final JdbcTemplate jdbc;

    public PostgresEvolutionAssessmentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<EvolutionAssessment> mapper =
            (r, n) ->
                    new EvolutionAssessment(
                            r.getObject("id", UUID.class),
                            r.getObject("patient_id", UUID.class),
                            r.getObject("weight_record_id", UUID.class),
                            r.getObject("front_photo_id", UUID.class),
                            r.getObject("side_photo_id", UUID.class),
                            r.getObject("back_photo_id", UUID.class),
                            r.getTimestamp("recorded_at").toInstant(),
                            r.getString("request_digest"));

    public Optional<EvolutionAssessment> findById(UUID id) {
        return jdbc.query("SELECT * FROM evolution_assessments WHERE id=?", mapper, id).stream()
                .findFirst();
    }

    public List<EvolutionAssessment> findByPatientId(UUID id) {
        return jdbc.query(
                "SELECT * FROM evolution_assessments WHERE patient_id=? ORDER BY recorded_at"
                    + " DESC,id",
                mapper,
                id);
    }

    public EvolutionAssessment save(EvolutionAssessment v) {
        jdbc.update(
                "INSERT INTO"
                    + " evolution_assessments(id,patient_id,weight_record_id,front_photo_id,side_photo_id,back_photo_id,recorded_at,request_digest)"
                    + " VALUES(?,?,?,?,?,?,?,?)",
                v.id(),
                v.patientId(),
                v.weightRecordId(),
                v.frontPhotoId(),
                v.sidePhotoId(),
                v.backPhotoId(),
                Timestamp.from(v.recordedAt()),
                v.requestDigest());
        return v;
    }

    public void delete(EvolutionAssessment value) {
        jdbc.update("DELETE FROM evolution_assessments WHERE id=?", value.id());
    }

    public boolean referencesPhoto(UUID id) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM evolution_assessments WHERE front_photo_id=?"
                            + " OR side_photo_id=? OR back_photo_id=?)",
                        Boolean.class,
                        id,
                        id,
                        id));
    }

    public boolean referencesWeight(UUID id) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM evolution_assessments WHERE"
                            + " weight_record_id=?)",
                        Boolean.class,
                        id));
    }
}
