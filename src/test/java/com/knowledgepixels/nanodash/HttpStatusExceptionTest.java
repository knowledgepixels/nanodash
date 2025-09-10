package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpStatusExceptionTest {

    @Test
    void testHttpStatusExceptionMessage() {
        int statusCode = 404;
        HttpStatusException exception = new HttpStatusException(statusCode);
        assertEquals("HTTP error: 404", exception.getMessage());
    }

}