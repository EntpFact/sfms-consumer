package com.hdfcbank.sfmsconsumer.dao;


import com.hdfcbank.sfmsconsumer.config.BTAllowedMsgType;
import com.hdfcbank.sfmsconsumer.model.AdmiTracker;
import com.hdfcbank.sfmsconsumer.model.MsgEventTracker;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static com.hdfcbank.sfmsconsumer.utils.Constants.CAPTURED;


@Slf4j
@Repository
@AllArgsConstructor
public class SFMSConsumerRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final BTAllowedMsgType btAllowedMsgType;


/*
    @Value("${db.msgeventtracker_insert_query}")
    private String msgEventTrackerInsertQuery;

    @Value("${db.admitracker_insert_query}")
    private String admiTrackerInsertQuery;

    @Value("${db.batchtracker_insert_query}")
    private String batchTrackerInsertQuery;

*/

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



/*    public void saveDataInMsgEventTracker(MsgEventTracker msgEventTracker) {
        String insertsql = "INSERT INTO network_il.msg_event_tracker (msg_id, source, target, batch_id, flow_type, msg_type, original_req, invalid_msg, replay_count, original_req_count, consolidate_amt, intermediate_req, intemdiate_count, status, batch_creation_Date, batch_timestamp, created_time, modified_timestamp, version) " +
                "VALUES (:msg_id, :source, :target, :batch_id, :flow_type, :msg_type, :original_req, :invalid_msg, :replay_count, :original_req_count, :consolidate_amt, :intermediate_req, :intemdiate_count, :status, :batch_creation_Date, :batch_timestamp, :created_time, :modified_timestamp,:version )";


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
        params.addValue("intemdiate_count", null);
        params.addValue("status", CAPTURED);
        params.addValue("batch_creation_Date", null);
        params.addValue("batch_timestamp", null);
        params.addValue("created_time", timestamp);
        params.addValue("modified_timestamp", null);
        params.addValue("version", 1.0);


        //create new entry for valid request /error request
        namedParameterJdbcTemplate.update(msgEventTrackerInsertQuery, params);


    }*/

    public void saveDataInAdmiTracker(AdmiTracker admiTracker){
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


/*    public void saveMsgInBatchTracker(BatchTracker batchTracker) {
        String sql = "INSERT INTO network_il.batch_tracker (batch_id, msg_id, msg_type, status, replay_count, created_time, modified_timestamp) " +
                "VALUES (:batch_id, :msg_id, :msg_type, :status, :replay_count, :created_time, :modified_timestamp)";
        LocalDateTime timestamp = LocalDateTime.now();

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("batch_id", batchTracker.getBatchId())
                .addValue("msg_id", batchTracker.getMsgId())
                .addValue("msg_type", batchTracker.getMsgType())
                .addValue("status", batchTracker.getStatus())
                .addValue("replay_count", 0)
                .addValue("created_time", timestamp)
                .addValue("modified_timestamp",null );

        namedParameterJdbcTemplate.update(sql, params);
    }*/

    public String checkNull(String req) {
        if (req != null)
            return req;

        return "";
    }

}
