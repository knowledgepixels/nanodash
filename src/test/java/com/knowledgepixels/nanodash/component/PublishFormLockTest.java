package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests for the parsing of the {@code locked} page parameter, which states that a pre-filled
 * value cannot be changed in the form (issue #678).
 */
class PublishFormLockTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI COMMENT = vf.createIRI(NP_URI + "/comment");

    private MockedStatic<TemplateData> templateDataMockedStatic;

    @BeforeEach
    void setUp() {
        new WicketTester(new WicketApplication());
        templateDataMockedStatic = mockStatic(TemplateData.class);
    }

    @AfterEach
    void tearDown() {
        templateDataMockedStatic.close();
    }

    private TemplateContext context() {
        // The lock parsing only touches parameter names, so a stub template is enough here;
        // the per-repetition behaviour on real templates is covered by LockedFieldTest.
        Template template = mock(Template.class);
        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);
        return new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
    }

    /**
     * Applies the given {@code locked} values to three prepared contexts, each holding the same
     * two parameters, and returns them as [assertion, provenance, pubinfo].
     */
    private TemplateContext[] applyLocks(String... lockedValues) {
        TemplateContext assertion = context();
        TemplateContext provenance = context();
        TemplateContext pubinfo = context();
        for (TemplateContext c : new TemplateContext[]{assertion, provenance, pubinfo}) {
            c.setParam("comment", "first");
            c.setParam("comment__1", "second");
        }
        Map<Integer, TemplateContext> piParamIdMap = new HashMap<>();
        piParamIdMap.put(1, pubinfo);
        PageParameters params = new PageParameters();
        for (String v : lockedValues) params.add("locked", v);
        PublishForm.applyLocks(params, assertion, provenance, piParamIdMap);
        return new TemplateContext[]{assertion, provenance, pubinfo};
    }

    /**
     * Same, for the statement lock: statement names need no value in the context, so the contexts
     * are handed the names straight from the parameter.
     */
    private TemplateContext[] applyStatementLocks(String... lockedValues) {
        TemplateContext assertion = context();
        TemplateContext provenance = context();
        TemplateContext pubinfo = context();
        Map<Integer, TemplateContext> piParamIdMap = new HashMap<>();
        piParamIdMap.put(1, pubinfo);
        PageParameters params = new PageParameters();
        for (String v : lockedValues) params.add("locked-statements", v);
        PublishForm.applyLocks(params, assertion, provenance, piParamIdMap);
        return new TemplateContext[]{assertion, provenance, pubinfo};
    }

    @Test
    void statementLocksGoToTheirOwnContext() {
        TemplateContext[] c = applyStatementLocks("st1,prparam_st2", "piparam1_st3");
        assertTrue(c[0].isStatementLocked(vf.createIRI(NP_URI + "/st1")));
        assertFalse(c[0].isStatementLocked(vf.createIRI(NP_URI + "/st2")));
        assertTrue(c[1].isStatementLocked(vf.createIRI(NP_URI + "/st2")));
        assertTrue(c[2].isStatementLocked(vf.createIRI(NP_URI + "/st3")));
        assertFalse(c[2].isStatementLocked(vf.createIRI(NP_URI + "/st1")));
    }

    /**
     * The two parameters are independent: locking a value does not fix the repetitions of the
     * statement holding it, and locking a statement does not make its values uneditable.
     */
    @Test
    void theTwoLockParametersDoNotAffectEachOther() {
        TemplateContext assertion = context();
        assertion.setParam("comment", "a value");
        Map<Integer, TemplateContext> piParamIdMap = new HashMap<>();
        PageParameters params = new PageParameters();
        params.add("locked", "param_comment");
        PublishForm.applyLocks(params, assertion, context(), piParamIdMap);
        assertTrue(assertion.isLocked("comment"));
        assertFalse(assertion.isStatementLocked(vf.createIRI(NP_URI + "/st1")));

        TemplateContext other = context();
        other.setParam("comment", "a value");
        PageParameters stParams = new PageParameters();
        stParams.add("locked-statements", "st1");
        PublishForm.applyLocks(stParams, other, context(), piParamIdMap);
        assertTrue(other.isStatementLocked(vf.createIRI(NP_URI + "/st1")));
        assertFalse(other.isLocked("comment"));
    }

    @Test
    void bareNameLocksAssertionParam() {
        TemplateContext[] c = applyLocks("comment");
        assertTrue(c[0].isLocked("comment"));
        assertFalse(c[1].isLocked("comment"));
        assertFalse(c[2].isLocked("comment"));
    }

    @Test
    void prefixedNamesLockTheirOwnContext() {
        TemplateContext[] c = applyLocks("param_comment,prparam_comment__1,piparam1_comment");
        assertTrue(c[0].isLocked("comment"));
        assertFalse(c[0].isLocked("comment__1"));
        assertTrue(c[1].isLocked("comment__1"));
        assertFalse(c[1].isLocked("comment"));
        assertTrue(c[2].isLocked("comment"));
    }

    /**
     * A locked repetition must not lock the others: pre-filling and locking the keys a user
     * already has has to leave them free to add more (issue #678).
     */
    @Test
    void lockingOneRepetitionLeavesTheOthersEditable() {
        TemplateContext[] c = applyLocks("param_comment");
        assertTrue(c[0].isLocked(COMMENT));
        assertFalse(c[0].isLocked(vf.createIRI(NP_URI + "/comment__1")));
    }

    @Test
    void repeatedAndListedNamesAreBothAccepted() {
        TemplateContext[] c = applyLocks("param_comment", " param_comment__1 ");
        assertTrue(c[0].isLocked("comment"));
        assertTrue(c[0].isLocked("comment__1"));
    }

    /**
     * Locking a field the form has no value for would leave it empty and uneditable, which for a
     * required field means the form can never be published.
     */
    @Test
    void lockOnParamWithoutValueIsIgnored() {
        TemplateContext[] c = applyLocks("param_nosuchparam");
        assertFalse(c[0].isLocked("nosuchparam"));
    }

    @Test
    void emptyAndUnknownPubinfoLocksAreIgnored() {
        TemplateContext[] c = applyLocks("", "piparam9_comment,,param_comment");
        assertTrue(c[0].isLocked("comment"));
    }

}
