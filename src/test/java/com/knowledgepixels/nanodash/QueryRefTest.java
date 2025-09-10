package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueryRefTest {

    @Test
    void constructorWithNullQueryNameNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new QueryRef(null));
        assertThrows(IllegalArgumentException.class, () -> new QueryRef(""));
        assertThrows(IllegalArgumentException.class, () -> new QueryRef(" "));
    }

    @Test
    void constructorWithNameAndParams() {
        QueryRef queryRef = new QueryRef("test-query", Map.of("param1", "value1", "param2", "value2"));
        assertNotNull(queryRef);
    }

    @Test
    void constructorWithNameAndParam() {
        QueryRef queryRef = new QueryRef("test-query", "param1", "value1");
        assertNotNull(queryRef);
        assertEquals(1, queryRef.getParams().size());
        assertEquals("value1", queryRef.getParams().get("param1"));
    }

    @Test
    void constructorWithNameAndParamNameNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new QueryRef("test-query", null, "value1"));
        assertThrows(IllegalArgumentException.class, () -> new QueryRef("test-query", "", "value1"));
        assertThrows(IllegalArgumentException.class, () -> new QueryRef("test-query", " ", "value1"));
    }

    @Test
    void constructorWithName() {
        QueryRef queryRef = new QueryRef("test-query");
        assertNotNull(queryRef);
        assertTrue(queryRef.getParams().isEmpty());
    }

    @Test
    void getName() {
        QueryRef queryRef = new QueryRef("test-query");
        assertNotNull(queryRef.getName());
        assertEquals("test-query", queryRef.getName());
    }

    @Test
    void getParams() {
        QueryRef queryRef = new QueryRef("test-query", Map.of("param1", "value1"));
        assertNotNull(queryRef.getParams());
        assertEquals(1, queryRef.getParams().size());
        assertEquals("value1", queryRef.getParams().get("param1"));
    }

    @Test
    void getParam() {
        QueryRef queryRef = new QueryRef("test-query", Map.of("param1", "value1"));
        assertNotNull(queryRef.getParam("param1"));
        assertEquals("value1", queryRef.getParam("param1"));
        assertNull(queryRef.getParam("param2"));
    }

}