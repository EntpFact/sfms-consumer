package com.hdfcbank.sfmsconsumer.service;

import com.hdfcbank.sfmsconsumer.kafkaproducer.KafkaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublishMessage Tests")
class PublishMessageTest {

    @Mock
    private KafkaUtils kafkaUtils;

    @Mock
    private BatchIdXmlFieldExtractor batchIdXmlFieldExtractor;

    @InjectMocks
    private PublishMessage publishMessage;

    private String[] validXmlArray;
    private String[] emptyXmlArray;
    private String[] nullXmlArray;

    @BeforeEach
    void setUp() {
        // Set up the topic values using ReflectionTestUtils
        ReflectionTestUtils.setField(publishMessage, "nilrouter", "nilrouter");
        ReflectionTestUtils.setField(publishMessage, "msgEventTracker", "MSGEVENTTRACKERTOPIC");

        validXmlArray = new String[]{
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.09\">" +
                        "<AppHdr>" +
                        "<MsgDefIdr>pacs.008.001.09</MsgDefIdr>" +
                        "</AppHdr>" +
                        "<FIToFICstmrCdtTrf>" +
                        "<GrpHdr>" +
                        "<MsgId>BATCH123</MsgId>" +
                        "</GrpHdr>" +
                        "</FIToFICstmrCdtTrf>" +
                        "</Document>"
        };

        emptyXmlArray = new String[]{"", ""};
        nullXmlArray = new String[]{null, null};
    }

    @Test
    @DisplayName("Should publish message successfully when batch ID is extracted")
    void shouldPublishMessageSuccessfullyWhenBatchIdIsExtracted() throws Exception {
        // Given
        String expectedKey = "BATCH123";
        String expectedXmlMessage = validXmlArray[0] + validXmlArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(validXmlArray[1]))
                .thenReturn(expectedKey);

        // When
        publishMessage.sendRequest(validXmlArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(validXmlArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", expectedKey);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", expectedKey);
    }

    @Test
    @DisplayName("Should publish message with null key when batch ID extraction returns null")
    void shouldPublishMessageWithNullKeyWhenBatchIdExtractionReturnsNull() throws Exception {
        // Given
        String expectedXmlMessage = validXmlArray[0] + validXmlArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(validXmlArray[1]))
                .thenReturn(null);

        // When
        publishMessage.sendRequest(validXmlArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(validXmlArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", null);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", null);
    }

    @Test
    @DisplayName("Should publish message with empty key when batch ID extraction returns empty string")
    void shouldPublishMessageWithEmptyKeyWhenBatchIdExtractionReturnsEmptyString() throws Exception {
        // Given
        String expectedXmlMessage = validXmlArray[0] + validXmlArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(validXmlArray[1]))
                .thenReturn("");

        // When
        publishMessage.sendRequest(validXmlArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(validXmlArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "");
    }

    @Test
    @DisplayName("Should publish message with whitespace key when batch ID extraction returns whitespace")
    void shouldPublishMessageWithWhitespaceKeyWhenBatchIdExtractionReturnsWhitespace() throws Exception {
        // Given
        String expectedXmlMessage = validXmlArray[0] + validXmlArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(validXmlArray[1]))
                .thenReturn("  ");

        // When
        publishMessage.sendRequest(validXmlArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(validXmlArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "  ");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "  ");
    }

    @Test
    @DisplayName("Should publish error message when batch ID extraction throws exception")
    void shouldPublishErrorMessageWhenBatchIdExtractionThrowsException() throws Exception {
        // Given
        String expectedXmlMessage = validXmlArray[0] + validXmlArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(validXmlArray[1]))
                .thenThrow(new RuntimeException("Extraction failed"));

        // When
        publishMessage.sendRequest(validXmlArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(validXmlArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "error");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "error");
    }

    @Test
    @DisplayName("Should handle empty XML array")
    void shouldHandleEmptyXmlArray() throws Exception {
        // Given
        String[] emptyArray = new String[]{"", ""};
        String expectedXmlMessage = "";

        when(batchIdXmlFieldExtractor.extractFieldByFileType(""))
                .thenReturn("EMPTY_KEY");

        // When
        publishMessage.sendRequest(emptyArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType("");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "EMPTY_KEY");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "EMPTY_KEY");
    }

    @Test
    @DisplayName("Should handle null XML array")
    void shouldHandleNullXmlArray() throws Exception {
        // Given
        String[] nullArray = new String[]{null, null};
        String expectedXmlMessage = "nullnull";

        when(batchIdXmlFieldExtractor.extractFieldByFileType(null))
                .thenThrow(new RuntimeException("Null XML"));

        // When
        publishMessage.sendRequest(nullArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(null);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "error");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "error");
    }

    @Test
    @DisplayName("Should handle mixed null and empty XML array")
    void shouldHandleMixedNullAndEmptyXmlArray() throws Exception {
        // Given
        String[] mixedArray = new String[]{null, ""};
        String expectedXmlMessage = "null";

        when(batchIdXmlFieldExtractor.extractFieldByFileType(""))
                .thenReturn("MIXED_KEY");

        // When
        publishMessage.sendRequest(mixedArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType("");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "MIXED_KEY");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "MIXED_KEY");
    }

    @Test
    @DisplayName("Should handle XML array with special characters")
    void shouldHandleXmlArrayWithSpecialCharacters() throws Exception {
        // Given
        String[] specialCharArray = new String[]{
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<Document><AppHdr><MsgDefIdr>pacs.008.001.09</MsgDefIdr></AppHdr><FIToFICstmrCdtTrf><GrpHdr><MsgId>BATCH!@#$%^&*()</MsgId></GrpHdr></FIToFICstmrCdtTrf></Document>"
        };
        String expectedXmlMessage = specialCharArray[0] + specialCharArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(specialCharArray[1]))
                .thenReturn("BATCH!@#$%^&*()");

        // When
        publishMessage.sendRequest(specialCharArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(specialCharArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "BATCH!@#$%^&*()");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "BATCH!@#$%^&*()");
    }

    @Test
    @DisplayName("Should handle XML array with unicode characters")
    void shouldHandleXmlArrayWithUnicodeCharacters() throws Exception {
        // Given
        String[] unicodeArray = new String[]{
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<Document><AppHdr><MsgDefIdr>pacs.008.001.09</MsgDefIdr></AppHdr><FIToFICstmrCdtTrf><GrpHdr><MsgId>BATCH中文</MsgId></GrpHdr></FIToFICstmrCdtTrf></Document>"
        };
        String expectedXmlMessage = unicodeArray[0] + unicodeArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(unicodeArray[1]))
                .thenReturn("BATCH中文");

        // When
        publishMessage.sendRequest(unicodeArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(unicodeArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "BATCH中文");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "BATCH中文");
    }

    @Test
    @DisplayName("Should handle very long XML array")
    void shouldHandleVeryLongXmlArray() throws Exception {
        // Given
        StringBuilder longXml = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longXml.append("<Element>Very long XML content</Element>");
        }
        String[] longArray = new String[]{"<?xml version=\"1.0\"?>", longXml.toString()};
        String expectedXmlMessage = longArray[0] + longArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(longArray[1]))
                .thenReturn("VERY_LONG_KEY");

        // When
        publishMessage.sendRequest(longArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(longArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "VERY_LONG_KEY");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "VERY_LONG_KEY");
    }

    @Test
    @DisplayName("Should handle XML array with different batch ID values")
    void shouldHandleXmlArrayWithDifferentBatchIdValues() throws Exception {
        // Given
        String[] xmlArray = new String[]{
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.002.001.12\">" +
                        "<AppHdr><MsgDefIdr>pacs.002.001.12</MsgDefIdr></AppHdr>" +
                        "<FIToFIPmtStsRpt><GrpHdr><MsgId>BATCH456</MsgId></GrpHdr></FIToFIPmtStsRpt></Document>"
        };
        String expectedXmlMessage = xmlArray[0] + xmlArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(xmlArray[1]))
                .thenReturn("BATCH456");

        // When
        publishMessage.sendRequest(xmlArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(xmlArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "BATCH456");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "BATCH456");
    }

    @Test
    @DisplayName("Should handle XML array with malformed XML")
    void shouldHandleXmlArrayWithMalformedXml() throws Exception {
        // Given
        String[] malformedArray = new String[]{
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<Document><AppHdr><MsgDefIdr>pacs.008.001.09</MsgDefIdr><FIToFICstmrCdtTrf><GrpHdr><MsgId>BATCH123</MsgId></GrpHdr></FIToFICstmrCdtTrf></Document>"
        };
        String expectedXmlMessage = malformedArray[0] + malformedArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(malformedArray[1]))
                .thenThrow(new RuntimeException("Malformed XML"));

        // When
        publishMessage.sendRequest(malformedArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(malformedArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "error");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "error");
    }

    @Test
    @DisplayName("Should handle XML array with empty first element")
    void shouldHandleXmlArrayWithEmptyFirstElement() throws Exception {
        // Given
        String[] emptyFirstArray = new String[]{"", "<Document><AppHdr><MsgDefIdr>pacs.008.001.09</MsgDefIdr></AppHdr><FIToFICstmrCdtTrf><GrpHdr><MsgId>BATCH123</MsgId></GrpHdr></FIToFICstmrCdtTrf></Document>"};
        String expectedXmlMessage = emptyFirstArray[0] + emptyFirstArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(emptyFirstArray[1]))
                .thenReturn("BATCH123");

        // When
        publishMessage.sendRequest(emptyFirstArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(emptyFirstArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "BATCH123");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "BATCH123");
    }

    @Test
    @DisplayName("Should handle XML array with empty second element")
    void shouldHandleXmlArrayWithEmptySecondElement() throws Exception {
        // Given
        String[] emptySecondArray = new String[]{"<?xml version=\"1.0\"?>", ""};
        String expectedXmlMessage = emptySecondArray[0] + emptySecondArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(emptySecondArray[1]))
                .thenReturn("EMPTY_SECOND_KEY");

        // When
        publishMessage.sendRequest(emptySecondArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(emptySecondArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "EMPTY_SECOND_KEY");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "EMPTY_SECOND_KEY");
    }

    @Test
    @DisplayName("Should handle XML array with null first element")
    void shouldHandleXmlArrayWithNullFirstElement() throws Exception {
        // Given
        String[] nullFirstArray = new String[]{null, "<Document><AppHdr><MsgDefIdr>pacs.008.001.09</MsgDefIdr></AppHdr><FIToFICstmrCdtTrf><GrpHdr><MsgId>BATCH123</MsgId></GrpHdr></FIToFICstmrCdtTrf></Document>"};
        String expectedXmlMessage = nullFirstArray[0] + nullFirstArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(nullFirstArray[1]))
                .thenReturn("BATCH123");

        // When
        publishMessage.sendRequest(nullFirstArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(nullFirstArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "BATCH123");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "BATCH123");
    }

    @Test
    @DisplayName("Should handle XML array with null second element")
    void shouldHandleXmlArrayWithNullSecondElement() throws Exception {
        // Given
        String[] nullSecondArray = new String[]{"<?xml version=\"1.0\"?>", null};
        String expectedXmlMessage = nullSecondArray[0] + nullSecondArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(null))
                .thenThrow(new RuntimeException("Null XML"));

        // When
        publishMessage.sendRequest(nullSecondArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(null);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "error");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "error");
    }

    @Test
    @DisplayName("Should handle XML array with whitespace elements")
    void shouldHandleXmlArrayWithWhitespaceElements() throws Exception {
        // Given
        String[] whitespaceArray = new String[]{"  ", "  "};
        String expectedXmlMessage = whitespaceArray[0] + whitespaceArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(whitespaceArray[1]))
                .thenReturn("WHITESPACE_KEY");

        // When
        publishMessage.sendRequest(whitespaceArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(whitespaceArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "WHITESPACE_KEY");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "WHITESPACE_KEY");
    }

    @Test
    @DisplayName("Should handle XML array with newline characters")
    void shouldHandleXmlArrayWithNewlineCharacters() throws Exception {
        // Given
        String[] newlineArray = new String[]{
                "<?xml version=\"1.0\"?>\n",
                "<Document>\n<AppHdr>\n<MsgDefIdr>pacs.008.001.09</MsgDefIdr>\n</AppHdr>\n<FIToFICstmrCdtTrf>\n<GrpHdr>\n<MsgId>BATCH123</MsgId>\n</GrpHdr>\n</FIToFICstmrCdtTrf>\n</Document>"
        };
        String expectedXmlMessage = newlineArray[0] + newlineArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(newlineArray[1]))
                .thenReturn("BATCH123");

        // When
        publishMessage.sendRequest(newlineArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(newlineArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "BATCH123");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "BATCH123");
    }

    @Test
    @DisplayName("Should handle XML array with tab characters")
    void shouldHandleXmlArrayWithTabCharacters() throws Exception {
        // Given
        String[] tabArray = new String[]{
                "<?xml version=\"1.0\"?>\t",
                "<Document>\t<AppHdr>\t<MsgDefIdr>pacs.008.001.09</MsgDefIdr>\t</AppHdr>\t<FIToFICstmrCdtTrf>\t<GrpHdr>\t<MsgId>BATCH123</MsgId>\t</GrpHdr>\t</FIToFICstmrCdtTrf>\t</Document>"
        };
        String expectedXmlMessage = tabArray[0] + tabArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(tabArray[1]))
                .thenReturn("BATCH123");

        // When
        publishMessage.sendRequest(tabArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(tabArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "BATCH123");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "BATCH123");
    }

    @Test
    @DisplayName("Should handle XML array with CDATA sections")
    void shouldHandleXmlArrayWithCDATASections() throws Exception {
        // Given
        String[] cdataArray = new String[]{
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<Document><AppHdr><MsgDefIdr><![CDATA[pacs.008.001.09]]></MsgDefIdr></AppHdr><FIToFICstmrCdtTrf><GrpHdr><MsgId><![CDATA[BATCH123]]></MsgId></GrpHdr></FIToFICstmrCdtTrf></Document>"
        };
        String expectedXmlMessage = cdataArray[0] + cdataArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(cdataArray[1]))
                .thenReturn("BATCH123");

        // When
        publishMessage.sendRequest(cdataArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(cdataArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "BATCH123");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "BATCH123");
    }

    @Test
    @DisplayName("Should handle XML array with comments")
    void shouldHandleXmlArrayWithComments() throws Exception {
        // Given
        String[] commentArray = new String[]{
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<Document><!-- This is a comment --><AppHdr><MsgDefIdr>pacs.008.001.09</MsgDefIdr></AppHdr><FIToFICstmrCdtTrf><GrpHdr><MsgId>BATCH123</MsgId></GrpHdr></FIToFICstmrCdtTrf></Document>"
        };
        String expectedXmlMessage = commentArray[0] + commentArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(commentArray[1]))
                .thenReturn("BATCH123");

        // When
        publishMessage.sendRequest(commentArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(commentArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "BATCH123");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "BATCH123");
    }

    @Test
    @DisplayName("Should handle XML array with processing instructions")
    void shouldHandleXmlArrayWithProcessingInstructions() throws Exception {
        // Given
        String[] piArray = new String[]{
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><?xml-stylesheet type=\"text/xsl\" href=\"style.xsl\"?>",
                "<Document><AppHdr><MsgDefIdr>pacs.008.001.09</MsgDefIdr></AppHdr><FIToFICstmrCdtTrf><GrpHdr><MsgId>BATCH123</MsgId></GrpHdr></FIToFICstmrCdtTrf></Document>"
        };
        String expectedXmlMessage = piArray[0] + piArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(piArray[1]))
                .thenReturn("BATCH123");

        // When
        publishMessage.sendRequest(piArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(piArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "BATCH123");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "BATCH123");
    }

    @Test
    @DisplayName("Should handle XML array with entity references")
    void shouldHandleXmlArrayWithEntityReferences() throws Exception {
        // Given
        String[] entityArray = new String[]{
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<Document><AppHdr><MsgDefIdr>pacs.008.001.09&amp;extra</MsgDefIdr></AppHdr><FIToFICstmrCdtTrf><GrpHdr><MsgId>BATCH123</MsgId></GrpHdr></FIToFICstmrCdtTrf></Document>"
        };
        String expectedXmlMessage = entityArray[0] + entityArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(entityArray[1]))
                .thenReturn("BATCH123");

        // When
        publishMessage.sendRequest(entityArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(entityArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "BATCH123");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "BATCH123");
    }

    @Test
    @DisplayName("Should handle XML array with different message types")
    void shouldHandleXmlArrayWithDifferentMessageTypes() throws Exception {
        // Given
        String[] differentMsgArray = new String[]{
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.002.001.12\">" +
                        "<AppHdr><MsgDefIdr>pacs.002.001.12</MsgDefIdr></AppHdr>" +
                        "<FIToFIPmtStsRpt><GrpHdr><MsgId>BATCH789</MsgId></GrpHdr></FIToFIPmtStsRpt></Document>"
        };
        String expectedXmlMessage = differentMsgArray[0] + differentMsgArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(differentMsgArray[1]))
                .thenReturn("BATCH789");

        // When
        publishMessage.sendRequest(differentMsgArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(differentMsgArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "BATCH789");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "BATCH789");
    }

    @Test
    @DisplayName("Should handle XML array with mixed content")
    void shouldHandleXmlArrayWithMixedContent() throws Exception {
        // Given
        String[] mixedContentArray = new String[]{
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<Document><AppHdr><MsgDefIdr>pacs.008.001.09<sub>sub</sub>suffix</MsgDefIdr></AppHdr><FIToFICstmrCdtTrf><GrpHdr><MsgId>BATCH123</MsgId></GrpHdr></FIToFICstmrCdtTrf></Document>"
        };
        String expectedXmlMessage = mixedContentArray[0] + mixedContentArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(mixedContentArray[1]))
                .thenReturn("BATCH123");

        // When
        publishMessage.sendRequest(mixedContentArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(mixedContentArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "BATCH123");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "BATCH123");
    }

    @Test
    @DisplayName("Should handle XML array with attributes")
    void shouldHandleXmlArrayWithAttributes() throws Exception {
        // Given
        String[] attributeArray = new String[]{
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<Document><AppHdr><MsgDefIdr id=\"123\" type=\"standard\">pacs.008.001.09</MsgDefIdr></AppHdr><FIToFICstmrCdtTrf><GrpHdr><MsgId>BATCH123</MsgId></GrpHdr></FIToFICstmrCdtTrf></Document>"
        };
        String expectedXmlMessage = attributeArray[0] + attributeArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(attributeArray[1]))
                .thenReturn("BATCH123");

        // When
        publishMessage.sendRequest(attributeArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(attributeArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "BATCH123");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "BATCH123");
    }

    @Test
    @DisplayName("Should handle XML array with nested elements")
    void shouldHandleXmlArrayWithNestedElements() throws Exception {
        // Given
        String[] nestedArray = new String[]{
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<Document><AppHdr><MsgDefIdr>pacs.008.001.09</MsgDefIdr><NestedElement><DeeplyNested><MsgDefIdr>different.value</MsgDefIdr></DeeplyNested></NestedElement></AppHdr><FIToFICstmrCdtTrf><GrpHdr><MsgId>BATCH123</MsgId></GrpHdr></FIToFICstmrCdtTrf></Document>"
        };
        String expectedXmlMessage = nestedArray[0] + nestedArray[1];

        when(batchIdXmlFieldExtractor.extractFieldByFileType(nestedArray[1]))
                .thenReturn("BATCH123");

        // When
        publishMessage.sendRequest(nestedArray);

        // Then
        verify(batchIdXmlFieldExtractor, times(1)).extractFieldByFileType(nestedArray[1]);
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "nilrouter", "BATCH123");
        verify(kafkaUtils, times(1)).publishToResponseTopic(expectedXmlMessage, "MSGEVENTTRACKERTOPIC", "BATCH123");
    }
}
