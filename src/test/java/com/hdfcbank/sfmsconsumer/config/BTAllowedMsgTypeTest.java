package com.hdfcbank.sfmsconsumer.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class BTAllowedMsgTypeTest {

    @Test
    void testDefaultAllowedMsgTypesIsEmpty() {
        BTAllowedMsgType btAllowedMsgType = new BTAllowedMsgType();
        assertNotNull(btAllowedMsgType.getAllowedMsgTypes());
        assertTrue(btAllowedMsgType.getAllowedMsgTypes().isEmpty());
    }

    @Test
    void testSetterAndGetter() {
        BTAllowedMsgType btAllowedMsgType = new BTAllowedMsgType();

        btAllowedMsgType.setAllowedMsgTypes(Arrays.asList("pacs.008", "pacs.002"));

        assertEquals(2, btAllowedMsgType.getAllowedMsgTypes().size());
        assertTrue(btAllowedMsgType.getAllowedMsgTypes().contains("pacs.008"));
        assertTrue(btAllowedMsgType.getAllowedMsgTypes().contains("pacs.002"));
    }

    @Test
    void testOverrideAllowedMsgTypes() {
        BTAllowedMsgType btAllowedMsgType = new BTAllowedMsgType();

        btAllowedMsgType.setAllowedMsgTypes(Arrays.asList("camt.054"));
        assertEquals(1, btAllowedMsgType.getAllowedMsgTypes().size());
        assertEquals("camt.054", btAllowedMsgType.getAllowedMsgTypes().get(0));
    }
}
