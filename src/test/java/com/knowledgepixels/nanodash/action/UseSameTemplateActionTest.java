package com.knowledgepixels.nanodash.action;

import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateData;
import com.knowledgepixels.nanodash.utils.TestUtils;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;

import static com.knowledgepixels.nanodash.utils.TestUtils.anyIri;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UseSameTemplateActionTest {

    private final MockedStatic<TemplateData> templateDataMockedStatic = mockStatic(TemplateData.class);

    @AfterEach
    void tearDown() {
        templateDataMockedStatic.close();
    }

    @Test
    void getLinkLabel() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub();
        String linkLabel = "create new with same template";
        UseSameTemplateAction action = new UseSameTemplateAction();
        String result = action.getLinkLabel(nanopub);
        assertEquals(linkLabel, result);
    }

    @Test
    void getTemplateUri() throws MalformedNanopubException {
        IRI mockedTemplateId = TestUtils.vf.createIRI("https://w3id.org/np/RAJetZMP40rNpwVYsUpYA5_psx-paQ6pf5Gu9iz9Vmwak");

        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(TestUtils.vf.createStatement(anyIri, anyIri, anyIri));
        creator.addProvenanceStatement(creator.getAssertionUri(), TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(creator.getNanopubUri(), Template.WAS_CREATED_FROM_TEMPLATE_PREDICATE, mockedTemplateId);

        Nanopub nanopub = creator.finalizeNanopub();

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplateId(nanopub)).thenReturn(mockedTemplateId);

        UseSameTemplateAction action = new UseSameTemplateAction();
        String result = action.getTemplateUri(nanopub);

        assertEquals(mockedTemplateId.stringValue(), result);
    }

    @Test
    void getParamString() throws MalformedNanopubException {
        Nanopub nanopub = TestUtils.createNanopub();
        UseSameTemplateAction action = new UseSameTemplateAction();
        String result = action.getParamString(nanopub);
        assertEquals("", result);
    }

    @Test
    void isApplicableToOwnNanopubs() {
        UseSameTemplateAction action = new UseSameTemplateAction();
        boolean result = action.isApplicableToOwnNanopubs();
        assertTrue(result);
    }

    @Test
    void isApplicableToOthersNanopubs() {
        UseSameTemplateAction action = new UseSameTemplateAction();
        boolean result = action.isApplicableToOthersNanopubs();
        assertTrue(result);
    }

    @Test
    void isApplicableToReturnsTrue() throws MalformedNanopubException {
        IRI mockedTemplateId = TestUtils.vf.createIRI("https://w3id.org/np/RAJetZMP40rNpwVYsUpYA5_psx-paQ6pf5Gu9iz9Vmwak");

        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(TestUtils.vf.createStatement(anyIri, anyIri, anyIri));
        creator.addProvenanceStatement(creator.getAssertionUri(), TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(creator.getNanopubUri(), Template.WAS_CREATED_FROM_TEMPLATE_PREDICATE, mockedTemplateId);

        Nanopub nanopub = creator.finalizeNanopub();

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplateId(nanopub)).thenReturn(mockedTemplateId);

        UseSameTemplateAction action = new UseSameTemplateAction();
        boolean result = action.isApplicableTo(nanopub);

        assertTrue(result);
    }

    @Test
    void isApplicableToReturnsFalse() throws MalformedNanopubException {
        IRI mockedTemplateId = TestUtils.vf.createIRI("https://w3id.org/np/RAJetZMP40rNpwVYsUpYA5_psx-paQ6pf5Gu9iz9Vmwak");

        NanopubCreator creator = TestUtils.getNanopubCreator();
        creator.addAssertionStatement(TestUtils.vf.createStatement(anyIri, anyIri, anyIri));
        creator.addProvenanceStatement(creator.getAssertionUri(), TestUtils.anyIri, TestUtils.anyIri);
        creator.addPubinfoStatement(creator.getNanopubUri(), Template.WAS_CREATED_FROM_TEMPLATE_PREDICATE, mockedTemplateId);

        Nanopub nanopub = creator.finalizeNanopub();

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplateId(nanopub)).thenReturn(null);

        UseSameTemplateAction action = new UseSameTemplateAction();
        boolean result = action.isApplicableTo(nanopub);

        assertFalse(result);
    }

}