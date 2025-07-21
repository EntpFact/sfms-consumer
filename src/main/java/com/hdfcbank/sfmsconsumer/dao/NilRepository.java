package com.hdfcbank.sfmsconsumer.dao;


import com.hdfcbank.sfmsconsumer.model.MsgEventTracker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Slf4j
@Repository
@EnableCaching
public class NilRepository {

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    public MsgEventTracker findByMsgId(String msgId) {
        String sql = "SELECT * FROM network_il.msg_event_tracker WHERE msg_id = :msgId";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("msgId", msgId);

        List<MsgEventTracker> result = namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
            MsgEventTracker tracker = new MsgEventTracker();
            tracker.setMsgId(rs.getString("msg_id"));
            tracker.setSource(rs.getString("source"));
            tracker.setTarget(rs.getString("target"));
            tracker.setFlowType(rs.getString("flow_type"));
            tracker.setMsgType(rs.getString("msg_type"));
            tracker.setOrgnlReq(rs.getString("original_req"));
            tracker.setOrgnlReqCount(rs.getInt("original_req_count"));
            tracker.setConsolidateAmt(rs.getBigDecimal("consolidate_amt"));
            tracker.setIntermediateReq(rs.getString("intermediate_req"));
            tracker.setIntermediateCount(rs.getInt("intemdiate_count"));
            tracker.setStatus(rs.getString("status"));
            tracker.setCreatedTime(rs.getObject("created_time", LocalDateTime.class));
            tracker.setModifiedTimestamp(rs.getObject("modified_timestamp", LocalDateTime.class));
            return tracker;
        });

        return result.isEmpty() ? null : result.get(0);
    }

    public void saveDuplicateEntry(MsgEventTracker tracker) {
        String selectSql = "SELECT MAX(version) FROM network_il.msg_dedup_tracker " +
                "WHERE msg_id = :msgId";

        MapSqlParameterSource baseParams = new MapSqlParameterSource();
        baseParams.addValue("msgId", tracker.getMsgId());


        BigDecimal currentVersion = namedParameterJdbcTemplate.queryForObject(
                selectSql, baseParams, BigDecimal.class);

        if (currentVersion != null) {
            // Row exists → update version
            BigDecimal nextVersion = currentVersion.add(BigDecimal.ONE);

            String updateSql = "UPDATE network_il.msg_dedup_tracker SET " +
                    "flow_type = :flowType, msg_type = :msgType, original_req = (XMLPARSE(CONTENT :originalReq)), " +
                    "version = :version, modified_timestamp = CURRENT_TIMESTAMP " +
                    "WHERE msg_id = :msgId AND source = :source AND target = :target";

            MapSqlParameterSource updateParams = new MapSqlParameterSource();
            updateParams.addValue("msgId", tracker.getMsgId());
            updateParams.addValue("source", tracker.getSource());
            updateParams.addValue("target", tracker.getTarget());
            updateParams.addValue("flowType", tracker.getFlowType());
            updateParams.addValue("msgType", tracker.getMsgType());
            updateParams.addValue("originalReq", tracker.getOrgnlReq());
            updateParams.addValue("version", nextVersion);

            namedParameterJdbcTemplate.update(updateSql, updateParams);

        } else {
            // Row does not exist → insert with version = 1
            String insertSql = "INSERT INTO network_il.msg_dedup_tracker " +
                    "(msg_id, source, target, flow_type, msg_type, original_req, version, created_time, modified_timestamp) " +
                    "VALUES (:msgId, :source, :target, :flowType, :msgType, (XMLPARSE(CONTENT :originalReq)), 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

            MapSqlParameterSource insertParams = new MapSqlParameterSource();
            insertParams.addValue("msgId", tracker.getMsgId());
            insertParams.addValue("source", tracker.getSource());
            insertParams.addValue("target", tracker.getTarget());
            insertParams.addValue("flowType", tracker.getFlowType());
            insertParams.addValue("msgType", tracker.getMsgType());
            insertParams.addValue("originalReq", tracker.getOrgnlReq());

            namedParameterJdbcTemplate.update(insertSql, insertParams);
        }
    }
}
