package com.knowledgepixels.nanodash.action;

import com.knowledgepixels.nanodash.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;

import static com.knowledgepixels.nanodash.action.NanopubAction.getEncodedUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentActionTest {

    @Test
    void getLinkLabel() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        String linkLabel = "comment";
        CommentAction action = new CommentAction();
        String result = action.getLinkLabel(nanopub);
        assertEquals(linkLabel, result);
    }

    @Test
    void getTemplateUri() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        CommentAction action = new CommentAction();
        String result = action.getTemplateUri(nanopub);
        assertEquals(CommentAction.TEMPLATE_URI, result);
    }

    @Test
    void getParamString() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        CommentAction action = new CommentAction();
        String result = action.getParamString(nanopub);
        String expectedUri = getEncodedUri(nanopub);
        assertEquals("param_thing=" + expectedUri, result);
    }

    @Test
    void isApplicableToOwnNanopubs() {
        CommentAction action = new CommentAction();
        boolean result = action.isApplicableToOwnNanopubs();
        assertTrue(result);
    }

    @Test
    void isApplicableToOthersNanopubs() {
        CommentAction action = new CommentAction();
        boolean result = action.isApplicableToOthersNanopubs();
        assertTrue(result);
    }

    @Test
    void isApplicableTo() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        Nanopub nanopub = TestUtils.createNanopub();
        CommentAction action = new CommentAction();
        boolean result = action.isApplicableTo(nanopub);
        assertTrue(result);
    }

}