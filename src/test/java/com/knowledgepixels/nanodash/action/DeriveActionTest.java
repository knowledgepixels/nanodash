package com.knowledgepixels.nanodash.action;

import com.knowledgepixels.nanodash.template.TemplateData;
import com.knowledgepixels.nanodash.utils.TestUtils;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import static com.knowledgepixels.nanodash.action.NanopubAction.getEncodedUri;
import static com.knowledgepixels.nanodash.utils.TestUtils.anyIri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class DeriveActionTest {

    private final MockedStatic<TemplateData> templateDataMockedStatic = mockStatic(TemplateData.class);

    @AfterEach
    void tearDown() {
        templateDataMockedStatic.close();
    }

    @Test
    void getLinkLabel() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub();
        String linkLabel = "edit as derived nanopublication";
        DeriveAction action = new DeriveAction();
        String result = action.getLinkLabel(nanopub);
        assertEquals(linkLabel, result);
    }

    @Test
    void getTemplateUriNanopubWithValidTemplate() throws MalformedNanopubException {
        IRI mockedTemplateId = TestUtils.vf.createIRI("https://w3id.org/np/RAJetZMP40rNpwVYsUpYA5_psx-paQ6pf5Gu9iz9Vmwak");

        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(TestUtils.vf.createStatement(anyIri, anyIri, anyIri));
        creator.addProvenanceStatement(creator.getAssertionUri(), TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(creator.getNanopubUri(), NTEMPLATE.WAS_CREATED_FROM_TEMPLATE, mockedTemplateId);

        Nanopub nanopub = creator.finalizeNanopub();

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplateId(nanopub)).thenReturn(mockedTemplateId);

        DeriveAction action = new DeriveAction();
        String result = action.getTemplateUri(nanopub);

        assertEquals(mockedTemplateId.stringValue(), result);
    }

    @Test
    void getTemplateUriNanopubWithInvalidTemplate() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub();
        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);

        DeriveAction action = new DeriveAction();
        String result = action.getTemplateUri(nanopub);

        assertEquals("http://purl.org/np/RACyK2NjqFgezYLiE8FQu7JI0xY1M1aNQbykeCW8oqXkA", result);
    }

    @Test
    void getParamString() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub();
        DeriveAction action = new DeriveAction();
        String result = action.getParamString(nanopub);
        String expectedUri = getEncodedUri(nanopub);
        assertEquals("derive=" + expectedUri, result);
    }

    @Test
    void isApplicableToOwnNanopubs() {
        DeriveAction action = new DeriveAction();
        boolean result = action.isApplicableToOwnNanopubs();
        assertTrue(result);
    }

    @Test
    void isApplicableToOthersNanopubs() {
        DeriveAction action = new DeriveAction();
        boolean result = action.isApplicableToOthersNanopubs();
        assertTrue(result);
    }

    @Test
    void isApplicableTo() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub();
        DeriveAction action = new DeriveAction();
        boolean result = action.isApplicableTo(nanopub);
        assertTrue(result);
    }

}