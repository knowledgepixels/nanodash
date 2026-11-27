package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.LocalUri;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.StatementItem;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.templates.Template;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * The read-only view of a nanopublication, filled by nanopub-java's matcher (#936): a statement
 * is shown once for each repetition the nanopublication has, each with its own values, what no
 * statement takes is left over, and the publication info templates share it out in turn.
 */
class ViewFillTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String LABELS = "https://w3id.org/np/RAViewFillTestLabelsTemplate0123456789abcdefg";
    private static final String LICENSE = "https://w3id.org/np/RAViewFillTestLicenseTemplate0123456789abcdef";
    private static final String GENERIC = "https://w3id.org/np/RAViewFillTestGenericTemplate0123456789abcdef";

    private static final IRI THING = vf.createIRI(LABELS + "/thing");
    private static final IRI NAME = vf.createIRI(LABELS + "/name");

    private static final IRI THING1 = vf.createIRI("http://example.com/thing1");
    private static final IRI THING2 = vf.createIRI("http://example.com/thing2");
    private static final IRI CC_BY = vf.createIRI("https://creativecommons.org/licenses/by/4.0/");

    private MockedStatic<TemplateData> templateDataMockedStatic;
    private TemplateData templateData;

    @BeforeEach
    void setUp() throws Exception {
        new WicketTester(new WicketApplication());
        templateDataMockedStatic = mockStatic(TemplateData.class);
        templateData = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateData);
        register(labelsTemplate());
        register(licenseTemplate());
        register(genericTemplate());
    }

    @AfterEach
    void tearDown() {
        templateDataMockedStatic.close();
    }

    private void register(Template template) {
        when(templateData.getTemplate(template.getId())).thenReturn(template);
    }

    private static NanopubCreator templateCreator(String uri, IRI type, String label) throws Exception {
        NanopubCreator creator = new NanopubCreator(uri);
        creator.setAssertionUri(uri + "/assertion");
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        creator.addAssertionStatement(creator.getAssertionUri(), RDF.TYPE, type);
        creator.addAssertionStatement(creator.getAssertionUri(), RDFS.LABEL, vf.createLiteral(label));
        return creator;
    }

    // An assertion template with one repeatable statement: a thing has a label.
    private static Template labelsTemplate() throws Exception {
        NanopubCreator creator = templateCreator(LABELS, NTEMPLATE.ASSERTION_TEMPLATE, "Labels");
        IRI st = vf.createIRI(LABELS + "/st1");
        creator.addAssertionStatement(creator.getAssertionUri(), NTEMPLATE.HAS_STATEMENT, st);
        creator.addAssertionStatement(st, RDF.TYPE, NTEMPLATE.REPEATABLE_STATEMENT);
        creator.addAssertionStatement(st, RDF.SUBJECT, THING);
        creator.addAssertionStatement(st, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st, RDF.OBJECT, NAME);
        creator.addAssertionStatement(THING, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(NAME, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        return new Template(creator.finalizeNanopub());
    }

    // A publication info template: the nanopublication has a license.
    private static Template licenseTemplate() throws Exception {
        NanopubCreator creator = templateCreator(LICENSE, NTEMPLATE.PUBINFO_TEMPLATE, "License");
        IRI st = vf.createIRI(LICENSE + "/st1");
        IRI license = vf.createIRI(LICENSE + "/license");
        creator.addAssertionStatement(creator.getAssertionUri(), NTEMPLATE.HAS_STATEMENT, st);
        creator.addAssertionStatement(st, RDF.SUBJECT, NTEMPLATE.NANOPUB_PLACEHOLDER);
        creator.addAssertionStatement(st, RDF.PREDICATE, DCTERMS.LICENSE);
        creator.addAssertionStatement(st, RDF.OBJECT, license);
        creator.addAssertionStatement(license, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        return new Template(creator.finalizeNanopub());
    }

    // A publication info template that takes any triple, like the one for statements written
    // by hand.
    private static Template genericTemplate() throws Exception {
        NanopubCreator creator = templateCreator(GENERIC, NTEMPLATE.PUBINFO_TEMPLATE, "Generic");
        IRI st = vf.createIRI(GENERIC + "/st1");
        IRI subj = vf.createIRI(GENERIC + "/subj");
        IRI pred = vf.createIRI(GENERIC + "/pred");
        IRI obj = vf.createIRI(GENERIC + "/obj");
        creator.addAssertionStatement(creator.getAssertionUri(), NTEMPLATE.HAS_STATEMENT, st);
        creator.addAssertionStatement(st, RDF.TYPE, NTEMPLATE.OPTIONAL_STATEMENT);
        creator.addAssertionStatement(st, RDF.TYPE, NTEMPLATE.REPEATABLE_STATEMENT);
        creator.addAssertionStatement(st, RDF.SUBJECT, subj);
        creator.addAssertionStatement(st, RDF.PREDICATE, pred);
        creator.addAssertionStatement(st, RDF.OBJECT, obj);
        creator.addAssertionStatement(subj, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(pred, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(obj, RDF.TYPE, NTEMPLATE.VALUE_PLACEHOLDER);
        return new Template(creator.finalizeNanopub());
    }

    private static Nanopub nanopub(List<Statement> assertion, List<Statement> pubinfo) throws Exception {
        String uri = "http://purl.org/nanopub/temp/viewfill/";
        NanopubCreator creator = new NanopubCreator(uri);
        creator.setAssertionUri(uri + "assertion");
        for (Statement st : assertion) creator.addAssertionStatement(st.getSubject(), st.getPredicate(), st.getObject());
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        for (Statement st : pubinfo) creator.addPubinfoStatement(st.getSubject(), st.getPredicate(), st.getObject());
        return creator.finalizeNanopub();
    }

    private static Object shown(TemplateContext context, IRI placeholder) {
        return context.getComponentModels().get(placeholder).getObject();
    }

    private static IRI repetition(IRI placeholder, int i) {
        return vf.createIRI(placeholder + "__" + i);
    }

    @Test
    void eachRepetitionIsShownWithItsOwnValues() throws Exception {
        Nanopub np = nanopub(List.of(
                vf.createStatement(THING2, RDFS.LABEL, vf.createLiteral("two")),
                vf.createStatement(THING1, RDFS.LABEL, vf.createLiteral("one"))), List.of());
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, LABELS, "statement", np);

        List<Statement> unused = new ViewFill(np).fillAssertion(context);

        StatementItem si = context.getStatementItems().get(0);
        assertTrue(si.isMatched());
        assertEquals(2, si.getRepetitionCount());
        assertEquals(THING1.stringValue(), shown(context, THING));
        assertEquals("\"one\"", shown(context, NAME));
        assertEquals(THING2.stringValue(), shown(context, repetition(THING, 1)));
        assertEquals("\"two\"", shown(context, repetition(NAME, 1)));
        assertTrue(unused.isEmpty(), unused.toString());
    }

    @Test
    void whatNoStatementTakesIsLeftOverAsTheViewShowsIt() throws Exception {
        Nanopub np = nanopub(List.of(
                vf.createStatement(THING1, RDFS.LABEL, vf.createLiteral("one")),
                vf.createStatement(vf.createIRI("http://purl.org/nanopub/temp/viewfill/"), RDFS.COMMENT, vf.createLiteral("about itself"))), List.of());
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, LABELS, "statement", np);

        List<Statement> unused = new ViewFill(np).fillAssertion(context);

        assertEquals(1, context.getStatementItems().get(0).getRepetitionCount());
        assertEquals(List.of(vf.createStatement(LocalUri.of("nanopub"), RDFS.COMMENT, vf.createLiteral("about itself"),
                LocalUri.of("assertion"))), unused);
    }

    @Test
    void aStatementTheNanopublicationHasNoTriplesForIsNotShown() throws Exception {
        Nanopub np = nanopub(List.of(vf.createStatement(THING1, RDFS.COMMENT, vf.createLiteral("no label"))), List.of());
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, LABELS, "statement", np);

        List<Statement> unused = new ViewFill(np).fillAssertion(context);

        assertFalse(context.getStatementItems().get(0).isMatched());
        assertEquals(1, unused.size());
    }

    @Test
    void eachPublicationInfoTemplateGetsWhatTheOnesBeforeItLeft() throws Exception {
        IRI npUri = vf.createIRI("http://purl.org/nanopub/temp/viewfill/");
        Nanopub np = nanopub(List.of(vf.createStatement(THING1, RDFS.LABEL, vf.createLiteral("one"))), List.of(
                vf.createStatement(npUri, DCTERMS.LICENSE, CC_BY),
                vf.createStatement(npUri, RDFS.COMMENT, vf.createLiteral("a comment"))));
        ViewFill viewFill = new ViewFill(np);
        TemplateContext license = new TemplateContext(ContextType.PUBINFO, LICENSE, "statement", np);
        TemplateContext generic = new TemplateContext(ContextType.PUBINFO, GENERIC, "statement", np);

        viewFill.fillPubinfo(license);
        viewFill.fillPubinfo(generic);

        assertTrue(license.getStatementItems().get(0).isMatched());
        assertEquals(CC_BY.stringValue(), shown(license, vf.createIRI(LICENSE + "/license")));
        // The generic template shows the comment and the seeAlso link, not the license again.
        StatementItem rest = generic.getStatementItems().get(0);
        assertEquals(np.getPubinfo().size() - 1, rest.getRepetitionCount());
        for (int i = 0; i < rest.getRepetitionCount(); i++) {
            IRI pred = vf.createIRI(GENERIC + "/pred" + (i == 0 ? "" : "__" + i));
            assertFalse(DCTERMS.LICENSE.stringValue().equals(shown(generic, pred)));
        }
    }

    @Test
    void aTemplateThatTakesAnyTripleIsTold() throws Exception {
        Nanopub np = nanopub(List.of(vf.createStatement(THING1, RDFS.LABEL, vf.createLiteral("one"))), List.of());
        assertTrue(ViewFill.isCatchAll(templateData.getTemplate(GENERIC), np));
        assertFalse(ViewFill.isCatchAll(templateData.getTemplate(LICENSE), np));
    }

}
