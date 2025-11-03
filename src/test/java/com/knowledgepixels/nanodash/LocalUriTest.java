package com.knowledgepixels.nanodash;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalUriTest {

    @Test
    void testLocalUriCreation() {
        LocalUri uri = LocalUri.of("example");
        assertEquals("local:example", uri.stringValue());
        assertEquals("example", uri.getLocalName());
        assertNull(uri.getNamespace());
    }

    @Test
    void testInvalidLocalUriCreation() {
        assertThrows(IllegalArgumentException.class, () -> LocalUri.of(null));
        assertThrows(IllegalArgumentException.class, () -> LocalUri.of(""));
        assertThrows(IllegalArgumentException.class, () -> LocalUri.of("   "));
    }

    @Test
    void equals() {
        LocalUri uri1 = LocalUri.of("example");
        LocalUri uri2 = LocalUri.of("example");
        LocalUri uri3 = LocalUri.of("different");
        IRI iri = Values.iri("local:example");

        assertEquals(uri1, uri2);
        assertNotEquals(uri1, uri3);
        assertEquals(iri, uri1);
    }

    @Test
    void testHashCode() {
        LocalUri uri1 = LocalUri.of("example");
        LocalUri uri2 = LocalUri.of("example");
        LocalUri uri3 = LocalUri.of("different");

        assertEquals(uri1.hashCode(), uri2.hashCode());
        assertNotEquals(uri1.hashCode(), uri3.hashCode());
    }

    @Test
    void stringValue() {
        LocalUri uri = LocalUri.of("example");
        assertEquals("local:example", uri.stringValue());
    }

    @Test
    void testCreationWithHashes() {
        assertThrows(IllegalArgumentException.class, () -> LocalUri.of("example#resource"));
        assertThrows(IllegalArgumentException.class, () -> LocalUri.of("example#"));
        assertThrows(IllegalArgumentException.class, () -> LocalUri.of("#example"));
    }

}