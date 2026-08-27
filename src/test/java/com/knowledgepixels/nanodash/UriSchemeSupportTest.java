package com.knowledgepixels.nanodash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the non-http(s) URI scheme support of issue #655.
 */
class UriSchemeSupportTest {

    private static final String DID = "did:plc:z72i7hdynmk6r22z27h6tvur";
    private static final String CID = "bafybeigdyrzt5sfp7udm7hu76uh7y26nf3efuylqabf3oclgtqy55fbzdi";
    private static final String AT_URI = "at://" + DID + "/app.bsky.feed.post/3k2akmn5c7l2v";

    @Test
    void isUriValueAcceptsTheNewSchemes() {
        assertTrue(Utils.isUriValue("https://w3id.org/np/RA123"));
        assertTrue(Utils.isUriValue("http://example.org/x"));
        assertTrue(Utils.isUriValue("ipfs://" + CID));
        assertTrue(Utils.isUriValue("ipns://example.org"));
        assertTrue(Utils.isUriValue(DID));
        assertTrue(Utils.isUriValue(AT_URI));
    }

    @Test
    void isUriValueRejectsNonUris() {
        assertFalse(Utils.isUriValue(null));
        assertFalse(Utils.isUriValue(""));
        assertFalse(Utils.isUriValue("   "));
        assertFalse(Utils.isUriValue("just some text"));
        assertFalse(Utils.isUriValue("localName"));
        // A scheme that is valid but not allowed in a nanopublication.
        assertFalse(Utils.isUriValue("ftp://example.org/x"));
        assertFalse(Utils.isUriValue("javascript:alert(1)"));
        // Not mistaken for a DID: the scheme is matched exactly, not by prefix.
        assertFalse(Utils.isUriValue("didsomething://example.org"));
    }

    @Test
    void isUriValueRejectsProseThatBeginsWithAnAllowedScheme() {
        // UriSchemes.isAllowedUriScheme alone accepts these, which would turn a literal into an
        // IRI at the call sites that decide between the two.
        assertFalse(Utils.isUriValue("at: home"));
        assertFalse(Utils.isUriValue("did: the thing"));
        assertFalse(Utils.isUriValue("http: //example.org"));
        // Nothing after the scheme is not a URI either.
        assertFalse(Utils.isUriValue("did:"));
        assertFalse(Utils.isUriValue("at:"));
        // A scheme and its slashes with nothing after them, which the "https?://.+" test that
        // this replaces also rejected. ParsedIRI does call these absolute, so isWellFormedUri
        // accepts them and isUriValue is what keeps them out.
        assertFalse(Utils.isUriValue("http://"));
        assertFalse(Utils.isUriValue("at://"));
        assertFalse(Utils.isUriValue("ipfs://"));
    }

    @Test
    void isWellFormedUriAcceptsAtUris() {
        // ParsedIRI cannot parse these: it reads "did:plc:..." as an authority with a bad port.
        assertTrue(Utils.isWellFormedUri(AT_URI));
        assertTrue(Utils.isWellFormedUri(DID));
        assertTrue(Utils.isWellFormedUri("ipfs://" + CID));
    }

    @Test
    void isWellFormedUriKeepsRejectingMalformedInput() {
        assertFalse(Utils.isWellFormedUri(null));
        assertFalse(Utils.isWellFormedUri(""));
        assertFalse(Utils.isWellFormedUri("not a uri"));
        assertFalse(Utils.isWellFormedUri("https://example.org/a b"));
        assertFalse(Utils.isWellFormedUri("relative/path"));
    }

    @Test
    void isWellFormedUriUnchangedForHttpInput() {
        assertTrue(Utils.isWellFormedUri("https://example.org/étude"));
        assertTrue(Utils.isWellFormedUri("https://example.org/p?q=1#f"));
        assertTrue(Utils.isWellFormedUri("http://example.org/a%20b"));
        assertFalse(Utils.isWellFormedUri("https://example.org/a|b"));
    }

    @Test
    void shortNameKeepsSchemeAndElidesOpaqueIdentifiers() {
        assertEquals("did:plc:z72i…tvur", Utils.getShortNameFromURI(DID));
        assertEquals("ipfs:bafy…bzdi", Utils.getShortNameFromURI("ipfs://" + CID));
        assertEquals("ipns:example.org", Utils.getShortNameFromURI("ipns://example.org"));
        // Short enough to show whole.
        assertEquals("did:web:short.io", Utils.getShortNameFromURI("did:web:short.io"));
    }

    @Test
    void shortNameOfAtUriIsTheRecordKey() {
        assertEquals("at:3k2akmn5c7l2v", Utils.getShortNameFromURI(AT_URI));
        assertEquals("at:did:plc:z72i…tvur", Utils.getShortNameFromURI("at://" + DID));
    }

    @Test
    void shortNameUnchangedForHttpUris() {
        assertEquals("any12345", Utils.getShortNameFromURI("http://knowledgepixels.com/resource#any12345"));
        assertEquals("doi:10.1234/x", Utils.getShortNameFromURI("https://doi.org/10.1234/x"));
    }

    @Test
    void ipfsUriWithPathStaysHierarchical() {
        assertEquals("readme.txt", Utils.getShortNameFromURI("ipfs://" + CID + "/docs/readme.txt"));
    }

    @Test
    void externalResolverUrlIsBuiltFromTheSchemeTemplate() {
        assertEquals("https://ipfs.io/ipfs/" + CID, Utils.getExternalResolverUrl("ipfs://" + CID));
        assertEquals("https://ipfs.io/ipns/example.org", Utils.getExternalResolverUrl("ipns://example.org"));
        // Colons and slashes are structural here and must survive encoding.
        assertEquals("https://dev.uniresolver.io/1.0/identifiers/" + DID, Utils.getExternalResolverUrl(DID));
        assertEquals("https://pdsls.dev/" + AT_URI, Utils.getExternalResolverUrl(AT_URI));
    }

    @Test
    void externalResolverUrlNotUsedForHttpOrUnknownSchemes() {
        assertNull(Utils.getExternalResolverUrl("https://example.org/x"));
        assertNull(Utils.getExternalResolverUrl("http://example.org/x"));
        assertNull(Utils.getExternalResolverUrl("ftp://example.org/x"));
        assertNull(Utils.getExternalResolverUrl("no-scheme"));
    }

    @Test
    void externalResolverUrlEscapesCharactersThatWouldRedirectTheResolver() {
        // A "?" or "#" left as-is would attach a query or fragment to the resolver URL rather
        // than being part of the identifier handed to it.
        assertEquals("https://ipfs.io/ipfs/abc%3Fx=1", Utils.getExternalResolverUrl("ipfs://abc?x=1"));
        assertEquals("https://ipfs.io/ipfs/abc%23frag", Utils.getExternalResolverUrl("ipfs://abc#frag"));
        // Existing percent-escapes are not doubled.
        assertEquals("https://ipfs.io/ipfs/a%20b", Utils.getExternalResolverUrl("ipfs://a%20b"));
    }

    @Test
    void sanitizerKeepsHrefsForTheNewSchemes() {
        assertTrue(Utils.sanitizeHtml("<a href=\"ipfs://" + CID + "\">x</a>").contains("ipfs://"));
        assertTrue(Utils.sanitizeHtml("<a href=\"" + DID + "\">x</a>").contains("did:plc:"));
        assertTrue(Utils.sanitizeHtml("<a href=\"" + AT_URI + "\">x</a>").contains("at://"));
        assertTrue(Utils.sanitizeHtml("<a href=\"https://example.org/\">x</a>").contains("https://example.org/"));
        assertTrue(Utils.sanitizeHtml("<a href=\"mailto:a@b.org\">x</a>").contains("mailto:"));
    }

    @Test
    void sanitizerStillDropsDangerousHrefs() {
        assertFalse(Utils.sanitizeHtml("<a href=\"javascript:alert(1)\">x</a>").contains("javascript:"));
        assertFalse(Utils.sanitizeHtml("<a href=\"ftp://example.org/\">x</a>").contains("ftp://"));
    }

    @Test
    void allowedSchemesLabelIsSortedAndComplete() {
        assertEquals("at, did, http, https, ipfs, ipns", Utils.getAllowedUriSchemesLabel());
    }

}
