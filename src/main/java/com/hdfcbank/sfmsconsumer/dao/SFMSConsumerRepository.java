package com.hdfcbank.sfmsconsumer.dao;


import com.hdfcbank.sfmsconsumer.config.BTAllowedMsgType;
import com.hdfcbank.sfmsconsumer.config.BypassProperties;
import com.hdfcbank.sfmsconsumer.exception.SFMSException;
import com.hdfcbank.sfmsconsumer.model.AdmiTracker;
import com.hdfcbank.sfmsconsumer.model.AuditStatus;
import com.hdfcbank.sfmsconsumer.model.MsgEventTracker;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;


@Slf4j
@Repository
@AllArgsConstructor
public class SFMSConsumerRepository {

    private final DatabaseClient databaseClient;
    private final BTAllowedMsgType btAllowedMsgType;
    private final BypassProperties bypassProperties;

    /**
     * Save message in MsgEventTracker & BatchTracker (reactive)
     */
    public Mono<AuditStatus> saveDataInMsgEventTracker(MsgEventTracker msgEventTracker) {
        LocalDateTime timestamp = LocalDateTime.now();
        boolean allowed = btAllowedMsgType.getAllowedMsgTypes().contains(msgEventTracker.getMsgType());

        String insertSql = (allowed) ? """
        WITH inserted_mvt_bt_event AS (
            INSERT INTO network_il.msg_event_tracker (
                msg_id, source, target, batch_id, flow_type, msg_type,
                original_req, invalid_msg, bypass_enabled, replay_count, original_req_count,
                consolidate_amt, intermediate_req, intemdiate_count, status,
                batch_creation_date, batch_timestamp, created_time, modified_timestamp, version
            )
            VALUES (:msg_id, :source, :target, :batch_id, :flow_type, :msg_type,
                    :original_req, :invalid_msg, :bypass_enabled, :replay_count, :original_req_count,
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
            original_req, invalid_msg, bypass_enabled, replay_count, original_req_count,
            consolidate_amt, intermediate_req, intemdiate_count, status,
            batch_creation_date, batch_timestamp, created_time, modified_timestamp, version
        )
        VALUES (:msg_id, :source, :target, :batch_id, :flow_type, :msg_type, 
                :original_req, :invalid_msg, :bypass_enabled, :replay_count, :original_req_count, 
                :consolidate_amt, :intermediate_req, :intemdiate_count, :status, 
                :batch_creation_date, :batch_timestamp, :created_time, :modified_timestamp, :version)
        """;

        String duplicateCheckSql = """
        SELECT status
        FROM network_il.msg_event_tracker
        WHERE msg_id = :msg_id
          AND source = :source
          AND target = :target
          AND batch_id = :batch_id
        LIMIT 1
        """;

        //  Step 1: Safe duplicate check with bindNull for batch_id
        DatabaseClient.GenericExecuteSpec duplicateSpec = databaseClient.sql(duplicateCheckSql)
                .bind("msg_id", msgEventTracker.getMsgId())
                .bind("source", msgEventTracker.getSource())
                .bind("target", msgEventTracker.getTarget());

        String batchId = msgEventTracker.getBatchId();
        if (batchId == null || batchId.isBlank()) {
            duplicateSpec = duplicateSpec.bindNull("batch_id", String.class);
        } else {
            duplicateSpec = duplicateSpec.bind("batch_id", batchId);
        }

        return duplicateSpec
                .map(row -> row.get("status", String.class))
                .one()
                .flatMap(existingStatus -> {
                    if (existingStatus != null) {
                        if ("CAPTURED".equalsIgnoreCase(existingStatus)) {
                            log.warn("Duplicate CAPTURED record found for msgId: {}", msgEventTracker.getMsgId());
                            return Mono.just(AuditStatus.CAPTURED_DUPLICATE);
                        } else if ("SEND_TO_PROCESSOR".equalsIgnoreCase(existingStatus)) {
                            log.warn("Duplicate SEND_TO_PROCESSOR record found for msgId: {}", msgEventTracker.getMsgId());
                            return incrementMsgEventTrackerVersion(msgEventTracker.getMsgId())
                                    .doOnNext(rows -> log.info("Incremented version for msgId={} by {}", msgEventTracker.getMsgId(), rows))
                                    .thenReturn(AuditStatus.SEND_TO_PROCESSOR_DUPLICATE);
                        } else if ("SEND_TO_DISPATCHER".equalsIgnoreCase(existingStatus)) {
                            log.warn("Duplicate SEND_TO_DISPATCHER record found for msgId: {}", msgEventTracker.getMsgId());
                            return incrementMsgEventTrackerVersion(msgEventTracker.getMsgId())
                                    .doOnNext(rows -> log.info("Incremented version for msgId={} by {}", msgEventTracker.getMsgId(), rows))
                                    .thenReturn(AuditStatus.SEND_TO_DISPATCHER);
                        }
                    }

                    //  Step 2: Insert since no duplicate
                    DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(insertSql);
                    spec = bindInsertParams(spec, msgEventTracker, timestamp);

                    return spec.fetch()
                            .rowsUpdated()
                            .map(rows -> rows > 0 ? AuditStatus.SUCCESS : AuditStatus.ERROR);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    //  Step 3: Record not found at all → safe to insert
                    DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(insertSql);
                    spec = bindInsertParams(spec, msgEventTracker, timestamp);

                    return spec.fetch()
                            .rowsUpdated()
                            .map(rows -> rows > 0 ? AuditStatus.SUCCESS : AuditStatus.ERROR);
                }))
                .onErrorResume(ex -> {
                    String msg = ex.getMessage() != null ? ex.getMessage() : "Unknown database error";

                    if (ex instanceof io.r2dbc.spi.R2dbcNonTransientResourceException
                            || ex instanceof io.r2dbc.spi.R2dbcTransientResourceException
                            || msg.contains("Connection refused")
                            || msg.contains("Connection reset")
                            || msg.contains("Timed out")) {

                        log.error("Database is unreachable for msgId {}: {}", msgEventTracker.getMsgId(), msg);
                        return Mono.error(new SFMSException("DATABASE_DOWN", ex));
                    }

                    log.error("Database error for msg_event_tracker msgId {}: {}", msgEventTracker.getMsgId(), msg, ex);
                    return Mono.error(new SFMSException("DATABASE_ERROR: " + msg, ex));
                });
    }
    private DatabaseClient.GenericExecuteSpec bindInsertParams(DatabaseClient.GenericExecuteSpec spec,
                                                               MsgEventTracker msgEventTracker,
                                                               LocalDateTime timestamp) {
        spec = bindNullable(spec, "msg_id", msgEventTracker.getMsgId(), String.class);
        spec = bindNullable(spec, "source", msgEventTracker.getSource(), String.class);
        spec = bindNullable(spec, "target", msgEventTracker.getTarget(), String.class);
        spec = bindNullable(spec, "batch_id", msgEventTracker.getBatchId(), String.class);
        spec = bindNullable(spec, "flow_type", msgEventTracker.getFlowType(), String.class);
        spec = bindNullable(spec, "msg_type", msgEventTracker.getMsgType(), String.class);
        spec = bindNullable(spec, "original_req", msgEventTracker.getOrgnlReq(), String.class);
        spec = bindNullable(spec, "invalid_msg", msgEventTracker.getInvalidReq(), Boolean.class);
        spec = bindNullable(spec, "bypass_enabled", false, Boolean.class);
        spec = bindNullable(spec, "batch_creation_date", msgEventTracker.getBatchCreationDate(), Date.class);
        spec = bindNullable(spec, "batch_timestamp", msgEventTracker.getBatchCreationTimestamp(), LocalDateTime.class);

        return spec.bind("replay_count", 0)
                .bindNull("original_req_count", Integer.class)
                .bindNull("consolidate_amt", Integer.class)
                .bindNull("intermediate_req", String.class)
                .bindNull("intemdiate_count", Integer.class)
                .bind("status", "CAPTURED")
                .bind("created_time", timestamp)
                .bind("modified_timestamp", timestamp)
                .bind("version", 1.0);
    }




    /**
     * Save message in ADMI tracker (reactive)
     */
    public Mono<AuditStatus> saveDataInAdmiTracker(AdmiTracker admiTracker) {
        LocalDateTime timestamp = LocalDateTime.now();

        // Step 1 — Check for duplicates
        String duplicateCheckSql = """
            SELECT status
            FROM network_il.admi004_tracker
            WHERE msg_id = :msg_id
            LIMIT 1
            """;

        // Step 2 — Main insert query
        String insertSql = """
            INSERT INTO network_il.admi004_tracker (
                msg_id, msg_type, original_req, target, replay_count, status, invalid_msg, bypass_enabled,
                batch_creation_date, batch_timestamp, version, created_time, modified_timestamp
            ) VALUES (
                :msg_id, :msg_type, :original_req, :target, :replay_count, :status, :invalid_msg, :bypass_enabled,
                :batch_creation_date, :batch_timestamp, :version, :created_time, :modified_timestamp
            )
            """;

        return databaseClient.sql(duplicateCheckSql)
                .bind("msg_id", admiTracker.getMsgId())
                .map(row -> row.get("status", String.class))
                .one()
                .flatMap(existingStatus -> {
                    if (existingStatus != null) {
                        if ("CAPTURED".equalsIgnoreCase(existingStatus)) {
                            log.warn("Duplicate CAPTURED record found for msgId: {}", admiTracker.getMsgId());
                            return Mono.just(AuditStatus.CAPTURED_DUPLICATE);
                        } else if ("SEND_TO_PROCESSOR".equalsIgnoreCase(existingStatus)) {
                            log.warn("Duplicate SEND_TO_PROCESSOR record found for msgId: {}", admiTracker.getMsgId());
                            return incrementAdmiTrackerVersion(admiTracker.getMsgId())
                                    .thenReturn(AuditStatus.SEND_TO_PROCESSOR_DUPLICATE);
                        } else if ("SEND_TO_DISPATCHER".equalsIgnoreCase(existingStatus)) {
                            log.warn("Duplicate SEND_TO_DISPATCHER record found for msgId: {}", admiTracker.getMsgId());
                            return incrementAdmiTrackerVersion(admiTracker.getMsgId())
                                    .thenReturn(AuditStatus.SEND_TO_DISPATCHER);
                        }
                    }

                    // Step 3 — No duplicate, proceed with insert
                    DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(insertSql);

                    spec = bindNullable(spec, "msg_id", admiTracker.getMsgId(), String.class);
                    spec = bindNullable(spec, "msg_type", admiTracker.getMsgType(), String.class);
                    spec = bindNullable(spec, "original_req", admiTracker.getOrgnlReq(), String.class);
                    spec = bindNullable(spec, "target", admiTracker.getTarget(), String.class);
                    spec = spec.bind("replay_count", 0);
                    spec = spec.bind("status", "CAPTURED");
                    spec = bindNullable(spec, "invalid_msg", admiTracker.getInvalidReq(), Boolean.class);
                    spec = bindNullable(spec, "bypass_enabled", false, Boolean.class);
                    spec = bindNullable(spec, "batch_creation_date", admiTracker.getBatchCreationTimestamp(), LocalDateTime.class);
                    spec = bindNullable(spec, "batch_timestamp", admiTracker.getBatchCreationTimestamp(), LocalDateTime.class);
                    spec = spec.bind("version", 1.0)
                            .bind("created_time", timestamp)
                            .bind("modified_timestamp", timestamp);

                    return spec.fetch()
                            .rowsUpdated()
                            .map(rows -> rows > 0 ? AuditStatus.SUCCESS : AuditStatus.ERROR);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Step 4 — Insert new if record not found
                    DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(insertSql);

                    spec = bindNullable(spec, "msg_id", admiTracker.getMsgId(), String.class);
                    spec = bindNullable(spec, "msg_type", admiTracker.getMsgType(), String.class);
                    spec = bindNullable(spec, "original_req", admiTracker.getOrgnlReq(), String.class);
                    spec = bindNullable(spec, "target", admiTracker.getTarget(), String.class);
                    spec = spec.bind("replay_count", 0);
                    spec = spec.bind("status", "CAPTURED");
                    spec = bindNullable(spec, "invalid_msg", admiTracker.getInvalidReq(), Boolean.class);
                    spec = bindNullable(spec, "bypass_enabled", false, Boolean.class);
                    spec = bindNullable(spec, "batch_creation_date", admiTracker.getBatchCreationTimestamp(), LocalDateTime.class);
                    spec = bindNullable(spec, "batch_timestamp", admiTracker.getBatchCreationTimestamp(), LocalDateTime.class);
                    spec = spec.bind("version", 1.0)
                            .bind("created_time", timestamp)
                            .bind("modified_timestamp", timestamp);

                    return spec.fetch()
                            .rowsUpdated()
                            .map(rows -> rows > 0 ? AuditStatus.SUCCESS : AuditStatus.ERROR);
                }))
                .onErrorResume(ex -> {
                    String msg = ex.getMessage() != null ? ex.getMessage() : "Unknown database error";

                    if (ex instanceof R2dbcDataIntegrityViolationException || msg.contains("duplicate key")) {
                        log.warn("Duplicate key detected for msgId: {}", admiTracker.getMsgId());
                        return Mono.just(AuditStatus.DUPLICATE);
                    }

                    // Database connection issues
                    if (ex instanceof io.r2dbc.spi.R2dbcNonTransientResourceException
                            || ex instanceof io.r2dbc.spi.R2dbcTransientResourceException
                            || msg.contains("Connection refused")
                            || msg.contains("Connection reset")
                            || msg.contains("Timed out")) {
                        log.error("Database unreachable for msgId {}: {}", admiTracker.getMsgId(), msg);
                        return Mono.error(new SFMSException("DATABASE_DOWN", ex));
                    }

                    log.error("Database error for admi_tracker msgId {}: {}", admiTracker.getMsgId(), msg, ex);
                    return Mono.error(new SFMSException("DATABASE_ERROR: " + msg, ex));
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
                .map(Long::intValue)
                .onErrorMap(ex -> new SFMSException("DATABASE_ERROR: Failed to increment msg_event_tracker version", ex));
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
                .rowsUpdated()
                .map(Long::intValue)
                .onErrorMap(ex -> new SFMSException("DATABASE_ERROR: Failed to increment admi_tracker version", ex));
    }

    private <T> DatabaseClient.GenericExecuteSpec bindNullable(
            DatabaseClient.GenericExecuteSpec spec, String name, T value, Class<T> type) {
        if (value != null) {
            return spec.bind(name, value);
        } else {
            return spec.bindNull(name, type);
        }
    }


    /**
     * Save message in MsgEventTracker, Bypass enabled
     */
    public Mono<AuditStatus> saveDataInMsgEventTrackerByPassEnabled(MsgEventTracker msgEventTracker) {
        LocalDateTime timestamp = LocalDateTime.now();

        String sql = """
                INSERT INTO network_il.msg_event_tracker (
                    msg_id, source, target, batch_id, flow_type, msg_type,
                    original_req, invalid_msg,bypass_enabled, replay_count, original_req_count,
                    consolidate_amt, intermediate_req, intemdiate_count, status,
                    batch_creation_date, batch_timestamp, created_time, modified_timestamp, version
                )
                VALUES (:msg_id, :source, :target, :batch_id, :flow_type, :msg_type, 
                        :original_req, :invalid_msg, :bypass_enabled, :replay_count, :original_req_count, 
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
        spec = bindNullable(spec, "bypass_enabled", msgEventTracker.getBypassEnabled(), Boolean.class);
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
                .bind("version", 1);

        return spec.fetch()
                .rowsUpdated()
                .map(rows -> rows > 0 ? AuditStatus.SUCCESS : AuditStatus.DUPLICATE)
                .onErrorResume(ex -> {
                    String msg = ex.getMessage() != null ? ex.getMessage() : "Unknown database error";
                    if (ex instanceof R2dbcDataIntegrityViolationException || msg.contains("duplicate key")) {
                        log.warn("Duplicate key detected for msgId: {}", msgEventTracker.getMsgId());
                        return Mono.just(AuditStatus.DUPLICATE);
                    }

                    // Database down or connection lost scenarios
                    if (ex instanceof io.r2dbc.spi.R2dbcNonTransientResourceException
                            || ex instanceof io.r2dbc.spi.R2dbcTransientResourceException
                            || msg.contains("Connection refused")
                            || msg.contains("Connection reset")
                            || msg.contains("Timed out")) {

                        log.error("Database is unreachable for msgId {}: {}", msgEventTracker.getMsgId(), msg);
                        return Mono.error(new SFMSException("DATABASE_DOWN", ex));
                    }
                    log.error("Database error for msg_event_tracker msgId {}: {}", msgEventTracker.getMsgId(), msg, ex);
                    return Mono.error(new SFMSException("DATABASE_ERROR: " + msg, ex));
                });
    }

    /**
     * Save message in ADMI tracker (reactive)
     */
    public Mono<AuditStatus> saveDataInAdmiTrackerByPassEnabled(AdmiTracker admiTracker) {
        LocalDateTime timestamp = LocalDateTime.now();

        String sql = """
                    INSERT INTO network_il.admi004_tracker (
                        msg_id, msg_type, original_req, target, replay_count, status, invalid_msg, bypass_enabled,
                        batch_creation_date, batch_timestamp, version, created_time, modified_timestamp
                    ) VALUES (
                        :msg_id, :msg_type, :original_req, :target, :replay_count, :status, :invalid_msg, :bypass_enabled,
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
        spec = bindNullable(spec, "bypass_enabled", admiTracker.getBypassEnabled(), Boolean.class);
        spec = bindNullable(spec, "batch_creation_date", admiTracker.getBatchCreationTimestamp(), LocalDateTime.class);
        spec = bindNullable(spec, "batch_timestamp", admiTracker.getBatchCreationTimestamp(), LocalDateTime.class);

        spec = spec.bind("version", 1.0)
                .bind("created_time", timestamp)
                .bind("modified_timestamp", timestamp);

        return spec.fetch()
                .rowsUpdated()
                .map(rows -> rows > 0 ? AuditStatus.SUCCESS : AuditStatus.DUPLICATE)
                .onErrorResume(ex -> {
                    String msg = ex.getMessage() != null ? ex.getMessage() : "Unknown database error";
                    if (ex instanceof R2dbcDataIntegrityViolationException || msg.contains("duplicate key")) {
                        log.warn("Duplicate key detected for msgId: {}", admiTracker.getMsgId());
                        return Mono.just(AuditStatus.DUPLICATE);
                    }

                    // Database down or connection lost scenarios
                    if (ex instanceof io.r2dbc.spi.R2dbcNonTransientResourceException
                            || ex instanceof io.r2dbc.spi.R2dbcTransientResourceException
                            || msg.contains("Connection refused")
                            || msg.contains("Connection reset")
                            || msg.contains("Timed out")) {

                        log.error("Database is unreachable for msgId {}: {}", admiTracker.getMsgId(), msg);
                        return Mono.error(new SFMSException("DATABASE_DOWN", ex));
                    }
                    log.error("Database error for admi_tracker msgId {}: {}", admiTracker.getMsgId(), msg, ex);
                    return Mono.error(new SFMSException("DATABASE_ERROR: " + msg, ex));
                });
    }

    public Mono<AuditStatus> saveDataInInvalidPayload(String msgId, String msgType, String originalReq, String byPassTarget, Boolean bypassEnabled) {
        LocalDateTime timestamp = LocalDateTime.now();

        String sql = """
                    INSERT INTO network_il.invalid_payload (
                        msg_id, msg_type, original_req, by_pass_enabled, target, created_time
                    ) VALUES (
                        :msg_id, :msg_type, :original_req, :by_pass_enabled, :target, :created_time
                    )
                """;

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql);

        // Bind all fields with null safety
        spec = bindNullable(spec, "msg_id", msgId, String.class);
        spec = bindNullable(spec, "msg_type", msgType, String.class);
        spec = bindNullable(spec, "original_req", originalReq, String.class);
        spec = bindNullable(spec, "by_pass_enabled", bypassEnabled, Boolean.class);
        spec = bindNullable(spec, "target", byPassTarget, String.class);
        spec = spec.bind("created_time", timestamp);

        return spec.fetch()
                .rowsUpdated()
                .map(rows -> rows > 0 ? AuditStatus.SUCCESS : AuditStatus.ERROR)
                .onErrorResume(ex -> {
                    String msg = ex.getMessage() != null ? ex.getMessage() : "Unknown database error";
                    if (ex instanceof R2dbcDataIntegrityViolationException || msg.contains("duplicate key")) {
                        log.warn("Duplicate key detected for msgId: {}", msg);
                        return Mono.just(AuditStatus.DUPLICATE);
                    }

                    // Database down or connection lost scenarios
                    if (ex instanceof io.r2dbc.spi.R2dbcNonTransientResourceException
                            || ex instanceof io.r2dbc.spi.R2dbcTransientResourceException
                            || msg.contains("Connection refused")
                            || msg.contains("Connection reset")
                            || msg.contains("Timed out")) {

                        log.error("Database is unreachable for msgId {}: {}", msgId, msg);
                        return Mono.error(new SFMSException("DATABASE_DOWN", ex));
                    }
                    log.error("Database error for bypass_invalid_payload msgId {}: {}", msg, msg, ex);
                    return Mono.error(new SFMSException("DATABASE_ERROR: " + msg, ex));
                });
    }

    public Mono<Void> updateStatusToTecx(String msgId) {
        // Try updating in multiple tables; only one will succeed
        return updateIfExists("network_il.msg_event_tracker", msgId)
                .switchIfEmpty(updateIfExists("network_il.admi004_tracker", msgId))
                .onErrorResume(ex -> {
                    String msg = ex.getMessage() != null ? ex.getMessage() : "Unknown database error";
                    log.error("Failed to update TECX status for msgId {}: {}", msgId, ex.getMessage(), ex);
                    return Mono.error(new SFMSException("DATABASE_ERROR: " + msg, ex));
                })
                .then();
    }

    private Mono<Void> updateIfExists(String tableName, String msgId) {


        String sql = "UPDATE " + tableName + " SET status = 'TECX' WHERE msg_id = :msg_id";
        return databaseClient.sql(sql)
                .bind("msg_id", msgId)
                .fetch()
                .rowsUpdated()
                .filter(rows -> rows > 0)
                .doOnNext(rows -> log.info("Updated TECX status in {} for msgId {}", tableName, msgId))
                .then();
    }

    public Mono<AuditStatus> updateStatusToSendToProcessorDynamic(String msgId, String batchId, String msgType) {
        LocalDateTime timestamp = LocalDateTime.now();
        boolean allowed = btAllowedMsgType.getAllowedMsgTypes().contains(msgType);

        if (msgType != null && msgType.toUpperCase().startsWith("ADMI")) {
            String sql = """
            UPDATE network_il.admi004_tracker
            SET status = 'SEND_TO_PROCESSOR', modified_timestamp = :modified_timestamp
            WHERE msg_id = :msg_id
        """;

            return databaseClient.sql(sql)
                    .bind("msg_id", msgId)
                    .bind("modified_timestamp", timestamp)
                    .fetch()
                    .rowsUpdated()
                    .map(rows -> rows > 0 ? AuditStatus.SUCCESS : AuditStatus.ERROR)
                    .doOnNext(status -> log.info("ADMI tracker update result={} for msgId={}", status, msgId))
                    .onErrorResume(ex -> {
                        log.error("DB error (ADMI) for msgId={}: {}", msgId, ex.getMessage(), ex);
                        return Mono.just(AuditStatus.ERROR); // <--- avoids hanging
                    })
                    .timeout(Duration.ofSeconds(5), Mono.just(AuditStatus.ERROR)); // <--- safety timeout
        }

        String sql = allowed ? """
        WITH updated_event AS (
            UPDATE network_il.msg_event_tracker
            SET status = 'SEND_TO_PROCESSOR', modified_timestamp = :modified_timestamp
            WHERE msg_id = :msg_id AND batch_id = :batch_id
            RETURNING msg_id, batch_id
        )
        UPDATE network_il.batch_tracker bt
        SET status = 'SEND_TO_PROCESSOR', modified_timestamp = :modified_timestamp
        FROM updated_event ue
        WHERE bt.msg_id = ue.msg_id AND bt.batch_id = ue.batch_id
    """ : """
        UPDATE network_il.msg_event_tracker
        SET status = 'SEND_TO_PROCESSOR', modified_timestamp = :modified_timestamp
        WHERE msg_id = :msg_id AND batch_id = :batch_id
    """;

        return databaseClient.sql(sql)
                .bind("msg_id", msgId)
                .bind("batch_id", batchId)
                .bind("modified_timestamp", timestamp)
                .fetch()
                .rowsUpdated()
                .map(rows -> rows > 0 ? AuditStatus.SUCCESS : AuditStatus.ERROR)
                .doOnNext(status -> log.info("Tracker update result={} for msgId={} batchId={}", status, msgId, batchId))
                .onErrorResume(ex -> {
                    log.error("DB error for msgId={} batchId={}: {}", msgId, batchId, ex.getMessage(), ex);
                    return Mono.just(AuditStatus.ERROR);
                })
                .timeout(Duration.ofSeconds(5), Mono.just(AuditStatus.ERROR)); // ✅ short-circuit timeout
    }


    private Mono<AuditStatus> handleDbError(Throwable ex, String msgId, String tableName) {
        String errMsg = ex.getMessage() != null ? ex.getMessage() : "Unknown database error";

        boolean dbDown =
                ex instanceof io.r2dbc.spi.R2dbcNonTransientResourceException ||
                        ex instanceof io.r2dbc.spi.R2dbcTransientResourceException ||
                        errMsg.contains("Connection refused") ||
                        errMsg.contains("Connection reset") ||
                        errMsg.toLowerCase().contains("timeout");

        if (dbDown) {
            log.error(" Database unreachable while updating {} for msgId {}: {}", tableName, msgId, errMsg);
            return Mono.error(new SFMSException("DATABASE_DOWN", ex));
        }

        log.error(" Error updating {} for msgId {}: {}", tableName, msgId, errMsg, ex);
        return Mono.error(new SFMSException("DATABASE_ERROR: " + errMsg, ex));
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


