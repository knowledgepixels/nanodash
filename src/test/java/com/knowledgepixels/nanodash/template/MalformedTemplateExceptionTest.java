package com.knowledgepixels.nanodash.template;

import org.nanopub.templates.MalformedTemplateException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MalformedTemplateExceptionTest {

    @Test
    void malformedTemplateExceptionReturnsCorrectMessage() {
        String message = "Template is malformed";
        MalformedTemplateException exception = new MalformedTemplateException(message);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void malformedTemplateExceptionHandlesNullMessage() {
        MalformedTemplateException exception = new MalformedTemplateException(null);
        assertNull(exception.getMessage());
    }

}