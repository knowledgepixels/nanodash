package com.knowledgepixels.nanodash.action;

import com.knowledgepixels.nanodash.utils.TestUtils;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UseTemplateActionTest {

    @Test
    void getLinkLabel() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub();
        String linkLabel = "use template";
        UseTemplateAction action = new UseTemplateAction();
        String result = action.getLinkLabel(nanopub);
        assertEquals(linkLabel, result);
    }

    @Test
    void getTemplateUri() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub();
        UseTemplateAction action = new UseTemplateAction();
        String result = action.getTemplateUri(nanopub);
        assertEquals(TestUtils.NANOPUB_URI, result);
    }

    @Test
    void getParamString() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub();
        UseTemplateAction action = new UseTemplateAction();
        String result = action.getParamString(nanopub);
        assertEquals("", result);
    }

    @Test
    void isApplicableToOwnNanopubs() {
        UseTemplateAction action = new UseTemplateAction();
        boolean result = action.isApplicableToOwnNanopubs();
        assertTrue(result);
    }

    @Test
    void isApplicableToOthersNanopubs() {
        UseTemplateAction action = new UseTemplateAction();
        boolean result = action.isApplicableToOthersNanopubs();
        assertTrue(result);
    }

    @Test
    void isApplicableTo() throws MalformedNanopubException {
        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(TestUtils.anyIri, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addProvenanceStatement(creator.getAssertionUri(), TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(creator.getNanopubUri(), TestUtils.anyIri, TestUtils.anyIri);
        Nanopub nanopub = creator.finalizeNanopub();
        UseTemplateAction action = new UseTemplateAction();
        boolean result = action.isApplicableTo(nanopub);
        assertTrue(result);
    }

}