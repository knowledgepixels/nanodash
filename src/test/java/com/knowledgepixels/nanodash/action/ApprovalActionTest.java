package com.knowledgepixels.nanodash.action;

import com.knowledgepixels.nanodash.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;

import static com.knowledgepixels.nanodash.action.NanopubAction.getEncodedUri;
import static org.junit.jupiter.api.Assertions.*;

class ApprovalActionTest {

    @Test
    void getLinkLabel() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        String linkLabel = "approve/disapprove";
        ApprovalAction action = new ApprovalAction();
        String result = action.getLinkLabel(nanopub);
        assertEquals(linkLabel, result);
    }

    @Test
    void getTemplateUri() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        ApprovalAction action = new ApprovalAction();
        String result = action.getTemplateUri(nanopub);
        assertEquals(ApprovalAction.TEMPLATE_URI, result);
    }

    @Test
    void getParamString() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        ApprovalAction action = new ApprovalAction();
        String result = action.getParamString(nanopub);
        String expectedUri = getEncodedUri(nanopub);
        assertEquals("param_nanopub=" + expectedUri, result);
    }

    @Test
    void isApplicableToOwnNanopubs() {
        ApprovalAction action = new ApprovalAction();
        boolean result = action.isApplicableToOwnNanopubs();
        assertFalse(result);
    }

    @Test
    void isApplicableToOthersNanopubs() {
        ApprovalAction action = new ApprovalAction();
        boolean result = action.isApplicableToOthersNanopubs();
        assertTrue(result);
    }

    @Test
    void isApplicableTo() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        ApprovalAction action = new ApprovalAction();
        boolean result = action.isApplicableTo(nanopub);
        assertTrue(result);
    }

}