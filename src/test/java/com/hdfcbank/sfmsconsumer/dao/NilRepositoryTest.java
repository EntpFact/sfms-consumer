package com.hdfcbank.sfmsconsumer.dao;

import com.hdfcbank.sfmsconsumer.model.AdmiTracker;
import com.hdfcbank.sfmsconsumer.model.BatchTracker;
import com.hdfcbank.sfmsconsumer.model.MsgEventTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NilRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @InjectMocks
    private NilRepository nilRepository;

    private MsgEventTracker msgEventTracker;

    private AdmiTracker admiTracker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        msgEventTracker = new MsgEventTracker();
        msgEventTracker.setMsgId("MSG123");
        msgEventTracker.setSource("SOURCE");
        msgEventTracker.setTarget("TARGET");
        msgEventTracker.setBatchId("BATCH1");
        msgEventTracker.setMsgType("pacs.002");
        msgEventTracker.setOrgnlReq("<xml>req</xml>");
        msgEventTracker.setInvalidReq(true);

        admiTracker = AdmiTracker.builder()
                .msgId("MSG123")
                .msgType("admi.004.001.01")
                .orgnlReq("<xml>test</xml>")
                .target("TARGET")
                .build();
    }

    @Test
    void testSaveDataInMsgEventTracker_success() {
        when(namedParameterJdbcTemplate.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(1);

        nilRepository.saveDataInMsgEventTracker(msgEventTracker);

        // verify query executed
        verify(namedParameterJdbcTemplate, times(1))
                .update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    void testCheckNull_withNonNull_returnsSameString() {
        String result = nilRepository.checkNull("ABC");
        assert (result.equals("ABC"));
    }

    @Test
    void testCheckNull_withNull_returnsEmptyString() {
        String result = nilRepository.checkNull(null);
        assert (result.isEmpty());
    }

    @Test
    void testSaveDataInAdmiTracker_success() {
        // given
        when(namedParameterJdbcTemplate.update(any(String.class), any(MapSqlParameterSource.class)))
                .thenReturn(1);

        // when
        nilRepository.saveDataInAdmiTracker(admiTracker);

        // then
        verify(namedParameterJdbcTemplate, times(1))
                .update(any(String.class), any(MapSqlParameterSource.class));
    }

    @Test
    void testSaveDataInAdmiTracker_verifyParameters() {
        // given
        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        when(namedParameterJdbcTemplate.update(any(String.class), any(MapSqlParameterSource.class)))
                .thenReturn(1);

        // when
        nilRepository.saveDataInAdmiTracker(admiTracker);

        // then
        verify(namedParameterJdbcTemplate).update(any(String.class), captor.capture());
        MapSqlParameterSource params = captor.getValue();

        // Assertions on key params
        assert params.getValue("msg_id").equals("MSG123");
        assert params.getValue("msg_type").equals("admi.004.001.01");
        assert params.getValue("original_req").equals("<xml>test</xml>");
        assert params.getValue("target").equals("TARGET");
        assert params.getValue("replay_count").equals(0);
        assert params.getValue("status").equals("CAPTURED");
        assert params.getValue("version").equals(1.0);

        // created_time should not be null
        assert params.getValue("created_time") instanceof LocalDateTime;
    }

    @Test
    void testSaveMsgInBatchTracker_insertsCorrectValues() {
        // Arrange
        BatchTracker batchTracker = BatchTracker.builder()
                .batchId("BATCH123")
                .msgId("MSG123")
                .msgType("pacs.008.001.09")
                .status("CAPTURED")
                .build();

        // Act
        nilRepository.saveMsgInBatchTracker(batchTracker);

        // Assert
        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);

        verify(namedParameterJdbcTemplate, times(1))
                .update(eq("INSERT INTO network_il.batch_tracker (batch_id, msg_id, msg_type, status, replay_count, created_time, modified_timestamp) " +
                                "VALUES (:batch_id, :msg_id, :msg_type, :status, :replay_count, :created_time, :modified_timestamp)"),
                        captor.capture());

        MapSqlParameterSource params = captor.getValue();

        // Check mapped values
        assertEquals("BATCH123", params.getValue("batch_id"));
        assertEquals("MSG123", params.getValue("msg_id"));
        assertEquals("pacs.008.001.09", params.getValue("msg_type"));
        assertEquals("CAPTURED", params.getValue("status"));
        assertEquals(0, params.getValue("replay_count"));   // default replay count
        assertEquals(null, params.getValue("modified_timestamp")); // must be null

        // created_time should not be null (system-generated)
        Object createdTime = params.getValue("created_time");
        assertNotNull(createdTime);
    }
}
