package com.hdfcbank.sfmsconsumer.dao;

import com.hdfcbank.sfmsconsumer.config.BTAllowedMsgType;
import com.hdfcbank.sfmsconsumer.model.AdmiTracker;
import com.hdfcbank.sfmsconsumer.model.MsgEventTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SFMSConsumerRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Mock
    private BTAllowedMsgType btAllowedMsgType;

    @InjectMocks
    private SFMSConsumerRepository repository;

    private MsgEventTracker msgEventTracker;
    private AdmiTracker admiTracker;

    @BeforeEach
    void setUp() {
        msgEventTracker = MsgEventTracker.builder()
                .msgId("MSG123")
                .msgType("pacs.008")
                .source("SFMS")
                .target("TargetA")
                .batchId("BATCH01")
                .flowType("INWARD")
                .invalidReq(false)
                .transformedJsonReq("{json}")
                .orgnlReq("<xml>request</xml>")
                .build();

        admiTracker = AdmiTracker.builder()
                .msgId("ADMI001")
                .msgType("admi.004")
                .target("TargetB")
                .invalidReq(false)
                .transformedJsonReq("{admi}")
                .orgnlReq("<xml>admi</xml>")
                .build();
    }

    @Test
    void testSaveDataInMsgEventTracker_withAllowedMsgType_insertsIntoBothTables() {
        // Arrange
        when(btAllowedMsgType.getAllowedMsgTypes()).thenReturn(List.of("pacs.008"));

        // Act
        repository.saveDataInMsgEventTracker(msgEventTracker);

        // Assert
        verify(namedParameterJdbcTemplate, times(1))
                .update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    void testSaveDataInMsgEventTracker_withNotAllowedMsgType_insertsIntoMETOnly() {
        // Arrange
        when(btAllowedMsgType.getAllowedMsgTypes()).thenReturn(Collections.singletonList("someOtherType"));

        // Act
        repository.saveDataInMsgEventTracker(msgEventTracker);

        // Assert
        verify(namedParameterJdbcTemplate, times(1))
                .update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    void testSaveDataInAdmiTracker_insertsIntoAdmiTrackerTable() {
        // Act
        repository.saveDataInAdmiTracker(admiTracker);

        // Assert
        verify(namedParameterJdbcTemplate, times(1))
                .update(anyString(), any(MapSqlParameterSource.class));
    }

    @Test
    void testCheckNull_whenNotNull_returnsSameValue() {
        String result = repository.checkNull("value");
        org.junit.jupiter.api.Assertions.assertEquals("value", result);
    }

    @Test
    void testCheckNull_whenNull_returnsEmptyString() {
        String result = repository.checkNull(null);
        org.junit.jupiter.api.Assertions.assertEquals("", result);
    }
}
