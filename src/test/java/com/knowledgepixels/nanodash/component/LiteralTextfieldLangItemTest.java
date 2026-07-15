package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.TemplateData;
import com.knowledgepixels.nanodash.template.TemplateTestUtil;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.validation.Validatable;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Component-level tests for the language-tag dropdown of
 * language-tag-selectable literal placeholders (LiteralTextfieldItem).
 */
public class LiteralTextfieldLangItemTest {

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

    /**
     * Builds a one-statement template (subject rdfs:comment [comment]) and returns
     * an uninitialized context for it; call setParam before initStatements.
     */
    private TemplateContext contextFor(boolean selectable, String defaultTag, String... possibleTags) throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        IRI st1 = vf.createIRI(NP_URI + "/st1");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Component test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, vf.createIRI("http://example.com/subject"));
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.COMMENT);
        creator.addAssertionStatement(st1, RDF.OBJECT, COMMENT);
        creator.addAssertionStatement(COMMENT, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        if (selectable) {
            creator.addAssertionStatement(COMMENT, RDF.TYPE, Template.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER);
        }
        if (defaultTag != null) {
            creator.addAssertionStatement(COMMENT, NTEMPLATE.HAS_LANGUAGE_TAG, vf.createLiteral(defaultTag));
        }
        for (String tag : possibleTags) {
            creator.addAssertionStatement(COMMENT, Template.POSSIBLE_LANGUAGE_TAG, vf.createLiteral(tag));
        }
        creator.addAssertionStatement(COMMENT, RDFS.LABEL, vf.createLiteral("comment"));
        Template template = TemplateTestUtil.parseTemplate(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);

        return new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
    }

    @SuppressWarnings("unchecked")
    private IModel<String> langModel(TemplateContext context) {
        return (IModel<String>) context.getComponentModels().get(TemplateContext.getLanguageModelKey(COMMENT));
    }

    @Test
    void langModelCreatedAndChoiceRegistered() throws Exception {
        TemplateContext context = contextFor(true, null);
        context.initStatements();
        assertTrue(context.getComponentModels().containsKey(TemplateContext.getLanguageModelKey(COMMENT)));
        assertEquals(2, context.getComponents().size(), "text field and language choice must both be registered");
    }

    @Test
    void plainPlaceholderGetsNoLangModel() throws Exception {
        TemplateContext context = contextFor(false, null);
        context.initStatements();
        assertFalse(context.getComponentModels().containsKey(TemplateContext.getLanguageModelKey(COMMENT)));
        assertEquals(1, context.getComponents().size());
    }

    @Test
    void defaultTagPreselects() throws Exception {
        TemplateContext context = contextFor(true, "en");
        context.initStatements();
        assertEquals("en", langModel(context).getObject());
    }

    @Test
    void paramBeatsDefault() throws Exception {
        TemplateContext context = contextFor(true, "en");
        context.setParam("comment__lang", "fr");
        context.initStatements();
        assertEquals("fr", langModel(context).getObject());
    }

    @Test
    void noDefaultLeavesEmpty() throws Exception {
        TemplateContext context = contextFor(true, null);
        context.initStatements();
        assertEquals("", langModel(context).getObject());
    }

    @Test
    void removeFromContextDeregistersBothComponents() throws Exception {
        TemplateContext context = contextFor(true, null);
        context.initStatements();
        assertEquals(2, context.getComponents().size());
        LiteralTextfieldItem extra = new LiteralTextfieldItem("value", COMMENT, true, context);
        assertEquals(4, context.getComponents().size());
        extra.removeFromContext();
        assertEquals(2, context.getComponents().size());
    }

    @Test
    void validatorAcceptsFreeWellFormedTags() throws Exception {
        TemplateContext context = contextFor(true, null);
        context.initStatements();
        LiteralTextfieldItem item = new LiteralTextfieldItem("value", COMMENT, true, context);
        LiteralTextfieldItem.LangTagValidator validator = item.new LangTagValidator(null);
        for (String ok : new String[]{"en", "en-GB", "zh-Hant", "de"}) {
            Validatable<String> v = new Validatable<>(ok);
            validator.validate(v);
            assertTrue(v.isValid(), ok + " must be accepted");
        }
        for (String bad : new String[]{"x", "x!", "en-", "-en", "12"}) {
            Validatable<String> v = new Validatable<>(bad);
            validator.validate(v);
            assertFalse(v.isValid(), bad + " must be rejected");
        }
    }

    @Test
    void validatorEnforcesRestrictedSet() throws Exception {
        TemplateContext context = contextFor(true, null, "en", "de");
        context.initStatements();
        LiteralTextfieldItem item = new LiteralTextfieldItem("value", COMMENT, true, context);
        LiteralTextfieldItem.LangTagValidator validator = item.new LangTagValidator(List.of("en", "de"));
        Validatable<String> ok = new Validatable<>("de");
        validator.validate(ok);
        assertTrue(ok.isValid());
        Validatable<String> bad = new Validatable<>("fr");
        validator.validate(bad);
        assertFalse(bad.isValid());
        // Tags are compared normalized:
        Validatable<String> okNormalized = new Validatable<>("DE");
        validator.validate(okNormalized);
        assertTrue(okNormalized.isValid());
    }

    @Test
    void emptyTagIsLeftToRequiredCheck() throws Exception {
        TemplateContext context = contextFor(true, null);
        context.initStatements();
        LiteralTextfieldItem item = new LiteralTextfieldItem("value", COMMENT, true, context);
        LiteralTextfieldItem.LangTagValidator validator = item.new LangTagValidator(null);
        Validatable<String> v = new Validatable<>(null);
        validator.validate(v);
        assertTrue(v.isValid());
        assertNull(v.getValue());
    }

}
