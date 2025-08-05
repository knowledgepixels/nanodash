package com.knowledgepixels.nanodash.action;

import com.knowledgepixels.nanodash.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;

import static com.knowledgepixels.nanodash.action.NanopubAction.getEncodedUri;
import static org.junit.jupiter.api.Assertions.*;

class RetractionActionTest {

    @Test
    void getLinkLabel() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub();
        String linkLabel = "retract";
        RetractionAction action = new RetractionAction();
        String result = action.getLinkLabel(nanopub);
        assertEquals(linkLabel, result);
    }

    @Test
    void getTemplateUri() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub();
        RetractionAction action = new RetractionAction();
        String result = action.getTemplateUri(nanopub);
        assertEquals(RetractionAction.TEMPLATE_URI, result);
    }

    @Test
    void getParamString() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub();
        RetractionAction action = new RetractionAction();
        String result = action.getParamString(nanopub);
        String expectedUri = getEncodedUri(nanopub);
        assertEquals("param_nanopubToBeRetracted=" + expectedUri, result);
    }

    @Test
    void isApplicableToOwnNanopubs() {
        RetractionAction action = new RetractionAction();
        boolean result = action.isApplicableToOwnNanopubs();
        assertTrue(result);
    }

    @Test
    void isApplicableToOthersNanopubs() {
        RetractionAction action = new RetractionAction();
        boolean result = action.isApplicableToOthersNanopubs();
        assertFalse(result);
    }

    @Test
    void isApplicableTo() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub();
        RetractionAction action = new RetractionAction();
        boolean result = action.isApplicableTo(nanopub);
        assertTrue(result);
    }

}