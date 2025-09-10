package com.knowledgepixels.nanodash;

import com.knowledgepixels.nanodash.utils.TestUtils;
import jakarta.xml.bind.DatatypeConverter;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.vocabulary.NPX;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

class NanopubElementTest {

    private final File np = new File("src/test/resources/np-grlc-query.trig");

    @Test
    void getWithURI() throws MalformedNanopubException, IOException {
        try (MockedStatic<Utils> utilsMockedStatic = mockStatic(Utils.class)) {
            utilsMockedStatic.when(() -> Utils.getNanopub(any())).thenReturn(new NanopubImpl(np));
            NanopubElement nanopubElement = NanopubElement.get(TestUtils.NANOPUB_URI);
            assertNotNull(nanopubElement);
        }
    }

    /*@Test
    void getWithURIAndNullNanopub() {
        try (MockedStatic<Utils> utilsMockedStatic = mockStatic(Utils.class)) {
            utilsMockedStatic.when(() -> Utils.getNanopub(any())).thenReturn(null);
            assertThrows(IllegalArgumentException.class, () -> NanopubElement.get(TestUtils.NANOPUB_URI));
        }
    }*/

    @Test
    void getWithValidNanopub() throws MalformedNanopubException, IOException {
        NanopubImpl nanopub = new NanopubImpl(np);
        NanopubElement nanopubElement = NanopubElement.get(nanopub);
        assertNotNull(nanopubElement);
    }

    @Test
    void getWithNullNanopub() {
        Nanopub nanopub = null;
        assertThrows(IllegalArgumentException.class, () -> NanopubElement.get(nanopub));
    }

    @Test
    void getNanopub() throws MalformedNanopubException, IOException {
        NanopubImpl nanopub = new NanopubImpl(np);
        NanopubElement nanopubElement = NanopubElement.get(nanopub);
        assertEquals(nanopub, nanopubElement.getNanopub());
    }

    @Test
    void getUri() throws MalformedNanopubException, IOException {
        NanopubImpl nanopub = new NanopubImpl(np);
        NanopubElement nanopubElement = NanopubElement.get(nanopub);
        assertEquals(nanopub.getUri().stringValue(), nanopubElement.getUri());
    }

    @Test
    void getLabel() throws MalformedNanopubException, IOException {
        String nanopubLabel = "Get participation links";
        NanopubImpl nanopub = new NanopubImpl(np);
        NanopubElement nanopubElement = NanopubElement.get(nanopub);
        String label = nanopubElement.getLabel();
        assertEquals(nanopubLabel, label);

        // Call getLabel again to test caching
        String cachedLabel = nanopubElement.getLabel();
        assertEquals(label, cachedLabel);
    }

    @Test
    void getCreationTime() throws MalformedNanopubException, IOException {
        Calendar nanopubCreationTime = DatatypeConverter.parseDateTime("2025-06-03T10:39:46.322Z");
        NanopubImpl nanopub = new NanopubImpl(np);
        NanopubElement nanopubElement = NanopubElement.get(nanopub);
        Calendar creationTime = nanopubElement.getCreationTime();
        assertEquals(nanopubCreationTime, creationTime);

        // Call getCreationTime again to test caching
        Calendar cachedCreationTime = nanopubElement.getCreationTime();
        assertEquals(creationTime, cachedCreationTime);
    }

    @Test
    void seemsToHaveSignature() throws MalformedNanopubException, IOException {
        NanopubImpl nanopub = new NanopubImpl(np);
        NanopubElement nanopubElement = NanopubElement.get(nanopub);
        boolean seemsToHaveSignature = nanopubElement.seemsToHaveSignature();
        assertTrue(seemsToHaveSignature);

        // Call seemsToHaveSignature again to test caching
        boolean cachedSeemsToHaveSignature = nanopubElement.seemsToHaveSignature();
        assertTrue(cachedSeemsToHaveSignature);
    }

    @Test
    void getPubkey() throws MalformedNanopubException, IOException, MalformedCryptoElementException {
        NanopubImpl nanopub = new NanopubImpl(np);
        NanopubElement nanopubElement = NanopubElement.get(nanopub);
        String pubkey = nanopubElement.getPubkey();
        assertEquals(SignatureUtils.getSignatureElement(nanopub).getPublicKeyString(), pubkey);
    }

    @Test
    void getPubkeyHash() throws MalformedCryptoElementException, MalformedNanopubException, IOException {
        NanopubImpl nanopub = new NanopubImpl(np);
        NanopubElement nanopubElement = NanopubElement.get(nanopub);
        String pubkey = SignatureUtils.getSignatureElement(nanopub).getPublicKeyString();
        String pubkeyHash = Utils.createSha256HexHash(pubkey);
        assertEquals(pubkeyHash, nanopubElement.getPubkeyhash());
    }

    @Test
    void hasValidSignature() throws MalformedNanopubException, IOException {
        NanopubImpl nanopub = new NanopubImpl(np);
        NanopubElement nanopubElement = NanopubElement.get(nanopub);
        assertTrue(nanopubElement.hasValidSignature());
    }

    @Test
    void getSignerId() throws MalformedNanopubException, IOException, MalformedCryptoElementException {
        NanopubImpl nanopub = new NanopubImpl(np);
        NanopubElement nanopubElement = NanopubElement.get(nanopub);
        IRI signerId = SignatureUtils.getSignatureElement(nanopub).getSigners().iterator().next();
        assertEquals(signerId, nanopubElement.getSignerId());
    }

    @Test
    void getTypes() throws MalformedNanopubException, IOException {
        NanopubImpl nanopub = new NanopubImpl(np);
        NanopubElement nanopubElement = NanopubElement.get(nanopub);
        List<IRI> expectedTypes = List.of(NPX.EXAMPLE_NANOPUB);
        assertEquals(expectedTypes, nanopubElement.getTypes());
    }

}