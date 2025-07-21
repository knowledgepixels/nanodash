package com.knowledgepixels.nanodash.template;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UnificationExceptionTest {

    @Test
    void unificationException_returnsCorrectMessage() {
        String message = "Error occurred during unification";
        UnificationException exception = new UnificationException(message);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void unificationException_handlesNullMessage() {
        UnificationException exception = new UnificationException(null);
        assertNull(exception.getMessage());
    }

}