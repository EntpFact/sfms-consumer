package com.hdfcbank.sfmsconsumer.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XmlSanitizerTest {

    @Test
    void testNullInputReturnsEmptyString() {
        String result = XmlSanitizer.sanitize(null);
        assertEquals("", result);
    }

    @Test
    void testNoSignatureBlockRemovesNewlines() {
        String input = "<root>\n<child>value</child>\r\n</root>";
        String expected = "<root><child>value</child></root>";
        assertEquals(expected, XmlSanitizer.sanitize(input));
    }

    @Test
    void testSingleSignatureBlockPreservesNewlines() {
        String input = "<root>\n" +
                "<sig:XMLSgntrs>\nline1\nline2\n</sig:XMLSgntrs>\n" +
                "</root>";
        String result = XmlSanitizer.sanitize(input);
        // The newlines inside the first signature block should be preserved
        String expected = "<root><sig:XMLSgntrs>line1line2</sig:XMLSgntrs></root>";
        assertEquals(expected, result);
    }

    @Test
    void testMultipleSignatureBlocksOnlyFirstPreservesNewlines() {
        String input = "<root>\n" +
                "<sig:XMLSgntrs>\nfirstBlockLine1\nfirstBlockLine2\n</sig:XMLSgntrs>\n" +
                "<sig:XMLSgntrs>\nsecondBlockLine1\nsecondBlockLine2\n</sig:XMLSgntrs>\n" +
                "</root>";
        String result = XmlSanitizer.sanitize(input);
        // Second signature block should have newlines removed
        String expected = "<root><sig:XMLSgntrs>firstBlockLine1firstBlockLine2</sig:XMLSgntrs>" +
                "<sig:XMLSgntrs>secondBlockLine1secondBlockLine2</sig:XMLSgntrs></root>";
        assertEquals(expected, result);
    }

    @Test
    void testNestedSignatureBlocksOnlyOuterPreservesNewlines() {
        String input = "<sig:XMLSgntrs>\n" +
                "outerLine1\n" +
                "<sig:XMLSgntrs>\ninnerLine1\ninnerLine2\n</sig:XMLSgntrs>\n" +
                "outerLine2\n" +
                "</sig:XMLSgntrs>";
        String result = XmlSanitizer.sanitize(input);
        // Inner block should have newlines removed
        String expected = "<sig:XMLSgntrs>outerLine1<sig:XMLSgntrs>innerLine1innerLine2</sig:XMLSgntrs>outerLine2</sig:XMLSgntrs>";
        assertEquals(expected, result);
    }

    @Test
    void testRemovesLiteralEscapedNewlines() {
        String input = "<root>line1\\r\\nline2\\r\\n</root>";
        String expected = "<root>line1line2</root>";
        assertEquals(expected, XmlSanitizer.sanitize(input));
    }
}
