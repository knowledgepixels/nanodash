package com.knowledgepixels.nanodash;

import org.eclipse.rdf4j.model.Literal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Serialization and parsing of the literal form used by the publish form's value fields
 * (nt:ValuePlaceholder). The long-literal cases guard the stack overflow that a
 * regex-based implementation caused for SVG-sized values (issue #634).
 */
class LiteralSerializationTest {

    private static String longSvgLiteral() {
        StringBuilder sb = new StringBuilder("\"<svg xmlns=\\\"http://www.w3.org/2000/svg\\\" viewBox=\\\"0 0 1029 478\\\">");
        for (int i = 0; i < 400; i++) {
            sb.append("<path d=\\\"M0.074,-0L0.074,-0.657L0.285,-0.657Z\\\" style=\\\"fill:rgb(63,63,63);\\\"/>");
        }
        return sb.append("</svg>\"").toString();
    }

    @Test
    void plainLiteralRoundTrips() {
        Literal l = Utils.getParsedLiteral("\"hello\"");
        assertEquals("hello", l.stringValue());
        assertTrue(Utils.isValidLiteralSerialization("\"hello\""));
    }

    @Test
    void languageTaggedAndTypedLiteralsRoundTrip() {
        assertEquals("de", Utils.getParsedLiteral("\"Haus\"@de").getLanguage().orElse(null));
        Literal typed = Utils.getParsedLiteral("\"42\"^^<http://www.w3.org/2001/XMLSchema#integer>");
        assertEquals("42", typed.stringValue());
        assertEquals("http://www.w3.org/2001/XMLSchema#integer", typed.getDatatype().stringValue());
    }

    @Test
    void escapedQuotesAndBackslashesRoundTrip() {
        String value = "a \" b \\ c";
        String serialized = "\"" + Utils.getEscapedLiteralString(value) + "\"";
        // The escaping must survive validation — an unescaped inner quote would end the
        // literal early and make the rest unparseable.
        assertTrue(Utils.isValidLiteralSerialization(serialized), serialized);
        assertEquals(value, Utils.getParsedLiteral(serialized).stringValue());
    }

    @Test
    void longSvgLiteralIsValidatedWithoutOverflowing() {
        String serialized = longSvgLiteral();
        assertTrue(serialized.length() > 20_000);
        assertTrue(Utils.isValidLiteralSerialization(serialized));
        assertTrue(Utils.getParsedLiteral(serialized).stringValue().startsWith("<svg "));
    }

    @Test
    void longSvgLiteralWithLanguageTagAndDatatypeIsValidated() {
        String body = longSvgLiteral();
        assertTrue(Utils.isValidLiteralSerialization(body + "@en"));
        assertTrue(Utils.isValidLiteralSerialization(body + "^^<http://www.w3.org/2001/XMLSchema#string>"));
    }

    @Test
    void malformedSerializationsAreRejected() {
        assertFalse(Utils.isValidLiteralSerialization("no quotes"));
        assertFalse(Utils.isValidLiteralSerialization("\"unterminated"));
        assertFalse(Utils.isValidLiteralSerialization("\"inner \" quote\""));
        assertFalse(Utils.isValidLiteralSerialization("\"text\" trailing"));
        assertFalse(Utils.isValidLiteralSerialization("\"bad escape \\n\""));
        assertThrows(IllegalArgumentException.class, () -> Utils.getParsedLiteral("no quotes"));
    }

}
