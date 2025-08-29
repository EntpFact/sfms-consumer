package com.hdfcbank.sfmsconsumer.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XmlSanitizerTest {

    @Test
    void testSanitize_preservesSignatureBlockAndRemovesOutsideNewlines() {
        String input = "<RequestPayload><BizMsgIdr>RBIP202507236244045029</BizMsgIdr>\\n" +
                " <MsgDefIdr>pacs.008.001.09</MsgDefIdr>\\n" +
                " <BizSvc>NEFTFIToFICustomerCredit</BizSvc>\\n" +
                " <CreDt>2025-07-23T17:02:34Z</CreDt>" +
                "<sig:XMLSgntrs>abc\\nxyz</sig:XMLSgntrs></RequestPayload>";

        String expected = "<RequestPayload><BizMsgIdr>RBIP202507236244045029</BizMsgIdr> " +
                "<MsgDefIdr>pacs.008.001.09</MsgDefIdr> " +
                "<BizSvc>NEFTFIToFICustomerCredit</BizSvc> " +
                "<CreDt>2025-07-23T17:02:34Z</CreDt>" +
                "<sig:XMLSgntrs>abc\\nxyz</sig:XMLSgntrs></RequestPayload>";

        String result = XmlSanitizer.sanitize(input);

        assertEquals(expected, result);
    }
}
