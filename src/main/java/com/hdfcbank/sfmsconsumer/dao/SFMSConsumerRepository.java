package com.hdfcbank.sfmsconsumer.dao;


import com.hdfcbank.sfmsconsumer.config.BTAllowedMsgType;
import com.hdfcbank.sfmsconsumer.model.AdmiTracker;
import com.hdfcbank.sfmsconsumer.model.AuditStatus;
import com.hdfcbank.sfmsconsumer.model.MsgEventTracker;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;


@Slf4j
@Repository
@AllArgsConstructor
public class SFMSConsumerRepository {


    private final DatabaseClient databaseClient;
    private final BTAllowedMsgType btAllowedMsgType;




    /**
     * Save message in MsgEventTracker & BatchTracker (reactive)
     */
    public Mono<AuditStatus> saveDataInMsgEventTracker(MsgEventTracker msgEventTracker) {
        LocalDateTime timestamp = LocalDateTime.now();
        boolean allowed = btAllowedMsgType.getAllowedMsgTypes().contains(msgEventTracker.getMsgType());
//&& !msgEventTracker.getInvalidReq()
        String sql = (allowed )? """
            WITH inserted_mvt_bt_event AS (
                INSERT INTO network_il.msg_event_tracker (
                    msg_id, source, target, batch_id, flow_type, msg_type, 
                    original_req, invalid_msg, replay_count, original_req_count, 
                    consolidate_amt, intermediate_req, intemdiate_count, status, 
                    batch_creation_date, batch_timestamp, created_time, modified_timestamp, version
                )
                VALUES (:msg_id, :source, :target, :batch_id, :flow_type, :msg_type, 
                        :original_req, :invalid_msg, :replay_count, :original_req_count, 
                        :consolidate_amt, :intermediate_req, :intemdiate_count, :status, 
                        :batch_creation_date, :batch_timestamp, :created_time, :modified_timestamp, :version)
                RETURNING msg_id, msg_type, status, batch_id, replay_count, created_time, modified_timestamp
            )
            INSERT INTO network_il.batch_tracker (
                batch_id, msg_id, msg_type, status, replay_count, created_time, modified_timestamp
            )
            SELECT batch_id, msg_id, msg_type, status, :replay_count, :created_time, :modified_timestamp
            FROM inserted_mvt_bt_event
            """ : """
            INSERT INTO network_il.msg_event_tracker (
                msg_id, source, target, batch_id, flow_type, msg_type,
                original_req, invalid_msg, replay_count, original_req_count,
                consolidate_amt, intermediate_req, intemdiate_count, status,
                batch_creation_date, batch_timestamp, created_time, modified_timestamp, version
            )
            VALUES (:msg_id, :source, :target, :batch_id, :flow_type, :msg_type, 
                    :original_req, :invalid_msg, :replay_count, :original_req_count, 
                    :consolidate_amt, :intermediate_req, :intemdiate_count, :status, 
                    :batch_creation_date, :batch_timestamp, :created_time, :modified_timestamp, :version)
            """;

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql);

        spec = bindNullable(spec, "msg_id", msgEventTracker.getMsgId(), String.class);
        spec = bindNullable(spec, "source", msgEventTracker.getSource(), String.class);
        spec = bindNullable(spec, "target", msgEventTracker.getTarget(), String.class);
        spec = bindNullable(spec, "batch_id", msgEventTracker.getBatchId(), String.class);
        spec = bindNullable(spec, "flow_type", msgEventTracker.getFlowType(), String.class);
        spec = bindNullable(spec, "msg_type", msgEventTracker.getMsgType(), String.class);
        spec = bindNullable(spec, "original_req", msgEventTracker.getOrgnlReq(), String.class);
        spec = bindNullable(spec, "invalid_msg", msgEventTracker.getInvalidReq(), Boolean.class);
        spec = bindNullable(spec, "batch_creation_date", msgEventTracker.getBatchCreationDate(), Date.class);
        spec = bindNullable(spec, "batch_timestamp", msgEventTracker.getBatchCreationTimestamp(), LocalDateTime.class);

        spec = spec.bind("replay_count", 0)
                .bindNull("original_req_count", Integer.class)
                .bindNull("consolidate_amt", Integer.class)
                .bindNull("intermediate_req", String.class)
                .bindNull("intemdiate_count", Integer.class)
                .bind("status", "CAPTURED");

        spec = spec.bind("created_time", timestamp)
                .bind("modified_timestamp", timestamp)
                .bind("version", 1.0);

        return spec.fetch()
                .rowsUpdated()
                .map(rows -> rows > 0 ? AuditStatus.SUCCESS : AuditStatus.DUPLICATE)
                .onErrorResume(ex -> {
                    String message = ex.getMessage() != null ? ex.getMessage() : "";
                    if (ex instanceof R2dbcDataIntegrityViolationException || message.contains("duplicate key")) {
                        log.warn("Duplicate key detected for msgId: {}", msgEventTracker.getMsgId());
                        return Mono.just(AuditStatus.DUPLICATE);
                    }
                    log.error("DB insert error for msgId {}: {}", msgEventTracker.getMsgId(), message, ex);
                    return Mono.just(AuditStatus.ERROR);
                });
    }


    /**
     * Save message in ADMI tracker (reactive)
     */
    public Mono<AuditStatus> saveDataInAdmiTracker(AdmiTracker admiTracker) {
        LocalDateTime timestamp = LocalDateTime.now();

        String sql = """
        INSERT INTO network_il.admi004_tracker (
            msg_id, msg_type, original_req, target, replay_count, status, invalid_msg,
            batch_creation_date, batch_timestamp, version, created_time, modified_timestamp
        ) VALUES (
            :msg_id, :msg_type, :original_req, :target, :replay_count, :status, :invalid_msg,
            :batch_creation_date, :batch_timestamp, :version, :created_time, :modified_timestamp
        )
    """;

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql);

        spec = bindNullable(spec, "msg_id", admiTracker.getMsgId(), String.class);
        spec = bindNullable(spec, "msg_type", admiTracker.getMsgType(), String.class);
        spec = bindNullable(spec, "original_req", admiTracker.getOrgnlReq(), String.class);
        spec = bindNullable(spec, "target", admiTracker.getTarget(), String.class);
        spec = spec.bind("replay_count", 0);
        spec = spec.bind("status", "CAPTURED");
        spec = bindNullable(spec, "invalid_msg", admiTracker.getInvalidReq(), Boolean.class);

        spec = bindNullable(spec, "batch_creation_date", admiTracker.getBatchCreationTimestamp(), LocalDateTime.class);
        spec = bindNullable(spec, "batch_timestamp", admiTracker.getBatchCreationTimestamp(), LocalDateTime.class);

        spec = spec.bind("version", 1.0)
                .bind("created_time", timestamp)
                .bind("modified_timestamp", timestamp);

        return spec.fetch()
                .rowsUpdated()
                .map(rows -> rows > 0 ? AuditStatus.SUCCESS : AuditStatus.DUPLICATE)
                .onErrorResume(ex -> {
                    String message = ex.getMessage() != null ? ex.getMessage() : "";
                    if (ex instanceof R2dbcDataIntegrityViolationException || message.contains("duplicate key")) {
                        log.warn("Duplicate key detected for msgId: {}", admiTracker.getMsgId());
                        return Mono.just(AuditStatus.DUPLICATE);
                    }
                    log.error("DB insert error for msgId {}: {}", admiTracker.getMsgId(), message, ex);
                    return Mono.just(AuditStatus.ERROR);
                });
    }

    /**
     * Increment version for MsgEventTracker (reactive)
     */
    public Mono<Integer> incrementMsgEventTrackerVersion(String msgId) {
        String sql = """
                UPDATE network_il.msg_event_tracker
                SET version = version + 1, modified_timestamp = NOW()
                WHERE msg_id = :msgId
                """;

        return databaseClient.sql(sql)
                .bind("msgId", msgId)
                .fetch()
                .rowsUpdated()
                .map(Long::intValue) //convert Mono<Long> → Mono<Integer>
                .doOnNext(rows -> {
                    if (rows > 0)
                        log.info("Version incremented for msg_event_tracker: {}", msgId);
                    else
                        log.warn("No record found for msg_event_tracker: {}", msgId);
                });
    }

    /**
     * Increment version for AdmiTracker (reactive)
     */
    public Mono<Integer> incrementAdmiTrackerVersion(String msgId) {
        String sql = """
                UPDATE network_il.admi004_tracker
                SET version = version + 1, modified_timestamp = NOW()
                WHERE msg_id = :msgId
                """;

        return databaseClient.sql(sql)
                .bind("msgId", msgId)
                .fetch()
                .rowsUpdated()               // returns Mono<Long>
                .map(Long::intValue)         // convert to Mono<Integer>
                .doOnNext(rows -> {
                    if (rows > 0)
                        log.info("Version incremented for admi_tracker: {}", msgId);
                    else
                        log.warn("No record found for admi_tracker: {}", msgId);
                });
    }

    private String checkNull(String val) {
        return val == null ? "" : val;
    }

    private <T> DatabaseClient.GenericExecuteSpec bindNullable(
            DatabaseClient.GenericExecuteSpec spec, String name, T value, Class<T> type) {
        if (value != null) {
            return spec.bind(name, value);
        } else {
            return spec.bindNull(name, type);
        }
    }
}























/*

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final BTAllowedMsgType btAllowedMsgType;

    public void saveDataInMsgEventTracker(MsgEventTracker msgEventTracker) {
        try {
            String insertInMETAndBTSql = """
                    WITH inserted_mvt_bt_event AS (
                        INSERT INTO network_il.msg_event_tracker (
                            msg_id, source, target, batch_id, flow_type, msg_type, 
                            original_req, invalid_msg, replay_count, original_req_count, 
                            consolidate_amt, intermediate_req,transformed_json_req, intemdiate_count, status, 
                            batch_creation_date, batch_timestamp, created_time, 
                            modified_timestamp, version
                        )
                        VALUES (
                            :msg_id, :source, :target, :batch_id, :flow_type, :msg_type, 
                            :original_req, :invalid_msg, :replay_count, :original_req_count, 
                            :consolidate_amt, :intermediate_req, :transformed_json_req, :intemdiate_count, :status, 
                            :batch_creation_date, :batch_timestamp, :created_time, 
                            :modified_timestamp, :version
                        )
                        RETURNING msg_id, msg_type, status, batch_id,replay_count,created_time,modified_timestamp
                    )
                    INSERT INTO network_il.batch_tracker (
                        batch_id, msg_id, msg_type, status, replay_count, created_time, modified_timestamp 
                    )
                    SELECT 
                        batch_id, msg_id, msg_type, status, :replay_count, :created_time, :modified_timestamp
                    FROM inserted_mvt_bt_event
                    """;

            String insertInMETSql = "INSERT INTO network_il.msg_event_tracker (msg_id, source, target, batch_id, flow_type, msg_type, original_req,transformed_json_req, invalid_msg, replay_count, original_req_count, consolidate_amt, intermediate_req, intemdiate_count, status, batch_creation_date, batch_timestamp, created_time, modified_timestamp, version) " +
                    "VALUES (:msg_id, :source, :target, :batch_id, :flow_type, :msg_type, :original_req, :transformed_json_req, :invalid_msg, :replay_count, :original_req_count, :consolidate_amt, :intermediate_req, :intemdiate_count, :status, :batch_creation_date, :batch_timestamp, :created_time, :modified_timestamp,:version )";

            PGobject jsonObject = new PGobject();
            jsonObject.setType("json");
            jsonObject.setValue(msgEventTracker.getTransformedJsonReq());


            LocalDateTime timestamp = LocalDateTime.now();

            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("msg_id", msgEventTracker.getMsgId());
            params.addValue("source", checkNull(msgEventTracker.getSource()));
            params.addValue("target", checkNull(msgEventTracker.getTarget()));
            params.addValue("batch_id", checkNull(msgEventTracker.getBatchId()));
            params.addValue("flow_type", checkNull(msgEventTracker.getFlowType()));
            params.addValue("msg_type", checkNull(msgEventTracker.getMsgType()));
            params.addValue("original_req", msgEventTracker.getOrgnlReq());
            params.addValue("original_req_count", null);
            params.addValue("consolidate_amt", null);
            params.addValue("invalid_msg", msgEventTracker.getInvalidReq());
            params.addValue("replay_count", 0);
            params.addValue("intermediate_req", null);
            params.addValue("transformed_json_req", jsonObject);
            params.addValue("intemdiate_count", null);
            params.addValue("status", CAPTURED);
            params.addValue("batch_creation_date", null);
            params.addValue("batch_timestamp", null);
            params.addValue("created_time", timestamp);
            params.addValue("modified_timestamp", timestamp);
            params.addValue("version", 1.0);

            // values specific to batch_tracker
            params.addValue("bt_replay_count", 0);
            params.addValue("bt_timestamp", timestamp);
            params.addValue("bt_modtimestamp", timestamp);
            // Check against allowed list from application.yml
            if (btAllowedMsgType.getAllowedMsgTypes().contains(msgEventTracker.getMsgType())) {
                namedParameterJdbcTemplate.update(insertInMETAndBTSql, params);
            } else {
                namedParameterJdbcTemplate.update(insertInMETSql, params);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void saveDataInAdmiTracker(AdmiTracker admiTracker) {
        String sql = "INSERT INTO network_il.admi004_tracker (msg_id, msg_type, original_req, target, replay_count, status, invalid_msg, transformed_json_req, batch_creation_date, batch_timestamp , version, created_time, modified_timestamp) " +
                "VALUES (:msg_id, :msg_type, :original_req , :target, :replay_count, :status, :invalid_msg, :transformed_json_req, :batch_creation_date, :batch_timestamp, :version, :created_time, :modified_timestamp )";

        LocalDateTime timestamp = LocalDateTime.now();
        PGobject jsonObject = new PGobject();
        jsonObject.setType("json");
        try {
            jsonObject.setValue(admiTracker.getTransformedJsonReq());
        } catch (SQLException e) {
            log.error(String.valueOf(e));
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("msg_id", admiTracker.getMsgId());
        params.addValue("msg_type", admiTracker.getMsgType());
        params.addValue("original_req", admiTracker.getOrgnlReq());
        params.addValue("target", admiTracker.getTarget());
        params.addValue("replay_count", 0);
        params.addValue("status", CAPTURED);
        params.addValue("transformed_json_req", jsonObject);
        params.addValue("invalid_msg", admiTracker.getInvalidReq());
        params.addValue("batch_creation_date", null);
        params.addValue("batch_timestamp", null);
        params.addValue("version", 1.0);
        params.addValue("created_time", timestamp);
        params.addValue("modified_timestamp", timestamp);
        namedParameterJdbcTemplate.update(sql, params);

    }


    public String checkNull(String req) {
        if (req != null)
            return req;

        return "";
    }

    public void incrementMsgEventTrackerVersion(String msgId) {
        String sql = "UPDATE network_il.msg_event_tracker " +
                "SET version = version + 1, modified_timestamp = NOW() " +
                "WHERE msg_id = :msgId";

        Map<String, Object> params = new HashMap<>();
        params.put("msgId", msgId);

        int rows = namedParameterJdbcTemplate.update(sql, params);
        if (rows > 0) {
            log.info("Version incremented for msg_event_tracker with msgId: {}", msgId);
        } else {
            log.warn("No record found to update in msg_event_tracker for msgId: {}", msgId);
        }
    }

    public void incrementAdmiTrackerVersion(String msgId) {
        String sql = "UPDATE network_il.admi004_tracker " +
                "SET version = version + 1, modified_timestamp = NOW() " +
                "WHERE msg_id = :msgId";

        Map<String, Object> params = new HashMap<>();
        params.put("msgId", msgId);

        int rows = namedParameterJdbcTemplate.update(sql, params);
        if (rows > 0) {
            log.info("Version incremented for admi_tracker with msgId: {}", msgId);
        } else {
            log.warn("No record found to update in admi_tracker for msgId: {}", msgId);
        }
    }

*/


