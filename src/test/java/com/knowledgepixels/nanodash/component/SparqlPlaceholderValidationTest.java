package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.Template;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.TemplateData;
import com.knowledgepixels.nanodash.template.TemplateTestUtil;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.Validatable;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.NanopubImpl;
import org.nanopub.vocabulary.KPXL_GRLC;
import org.nanopub.vocabulary.NTEMPLATE;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * The SPARQL of a query is checked while it can still be corrected, rather than after it has
 * been published in a nanopublication that can no longer be edited (#615).
 */
class SparqlPlaceholderValidationTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI SPARQL_FIELD = vf.createIRI(NP_URI + "/sparql");
    private static final IRI DESCRIPTION_FIELD = vf.createIRI(NP_URI + "/description");

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
     * Builds a two-statement template in the shape of the query template: one long literal
     * held by kpxl_grlc:sparql, one held by dct:description.
     */
    private TemplateContext queryTemplateContext() throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        IRI query = vf.createIRI(NP_URI + "/query");
        IRI stSparql = vf.createIRI(NP_URI + "/st1");
        IRI stDescription = vf.createIRI(NP_URI + "/st2");
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Defining a query"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, stSparql);
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, stDescription);
        creator.addAssertionStatement(stSparql, RDF.SUBJECT, query);
        creator.addAssertionStatement(stSparql, RDF.PREDICATE, KPXL_GRLC.SPARQL);
        creator.addAssertionStatement(stSparql, RDF.OBJECT, SPARQL_FIELD);
        creator.addAssertionStatement(stDescription, RDF.SUBJECT, query);
        creator.addAssertionStatement(stDescription, RDF.PREDICATE, DCTERMS.DESCRIPTION);
        creator.addAssertionStatement(stDescription, RDF.OBJECT, DESCRIPTION_FIELD);
        creator.addAssertionStatement(query, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
        creator.addAssertionStatement(SPARQL_FIELD, RDF.TYPE, NTEMPLATE.LONG_LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(SPARQL_FIELD, RDFS.LABEL, vf.createLiteral("the SPARQL code of the query"));
        creator.addAssertionStatement(DESCRIPTION_FIELD, RDF.TYPE, NTEMPLATE.LONG_LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(DESCRIPTION_FIELD, RDFS.LABEL, vf.createLiteral("what the query does"));
        Template template = TemplateTestUtil.parseTemplate(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);

        return new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", (String) null);
    }

    /**
     * Runs every validator the field carries, the way the form does on submit.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Validatable<String> validate(LiteralTextfieldItem item, String value) {
        Validatable<String> v = new Validatable<>(value);
        for (IValidator validator : item.getTextComponent().getValidators()) {
            validator.validate(v);
        }
        return v;
    }

    // Which long literal holds SPARQL is knowable only from the template, so that is where
    // it is read from.
    @Test
    void onlyTheSparqlHoldingPlaceholderIsRecognised() throws Exception {
        Template template = queryTemplateContext().getTemplate();
        assertTrue(template.isSparqlPlaceholder(SPARQL_FIELD));
        assertFalse(template.isSparqlPlaceholder(DESCRIPTION_FIELD));
    }

    // The synthetic template above is only worth as much as it resembles the real one, so the
    // published "Defining a grlc query" template is checked as it actually is.
    @Test
    void theRealQueryTemplateHasItsSparqlFieldRecognised() throws Exception {
        Nanopub np = new NanopubImpl(new File("src/test/resources/np-grlc-query-template.trig"), RDFFormat.TRIG);
        Template template = TemplateTestUtil.parseTemplate(np);
        String base = np.getUri().stringValue() + "/";
        assertTrue(template.isSparqlPlaceholder(vf.createIRI(base + "sparql")));
        assertFalse(template.isSparqlPlaceholder(vf.createIRI(base + "description")));
        assertFalse(template.isSparqlPlaceholder(vf.createIRI(base + "title")));
        assertFalse(template.isSparqlPlaceholder(vf.createIRI(base + "endpoint")));
    }

    @Test
    void validSparqlPasses() throws Exception {
        TemplateContext context = queryTemplateContext();
        context.initStatements();
        LiteralTextfieldItem item = new LiteralTextfieldItem("value", SPARQL_FIELD, true, context);
        assertTrue(validate(item, "select ?thing where { ?thing ?p ?o }").isValid());
    }

    @Test
    void unparseableSparqlIsRejected() throws Exception {
        TemplateContext context = queryTemplateContext();
        context.initStatements();
        LiteralTextfieldItem item = new LiteralTextfieldItem("value", SPARQL_FIELD, true, context);
        assertFalse(validate(item, "select ?thing where { ?thing").isValid());
    }

    // The bug that started this: a non-breaking space where a plain space belongs.
    @Test
    void sparqlWithAnInvisibleCharacterIsRejected() throws Exception {
        TemplateContext context = queryTemplateContext();
        context.initStatements();
        LiteralTextfieldItem item = new LiteralTextfieldItem("value", SPARQL_FIELD, true, context);
        // A non-breaking space where a plain space belongs.
        Validatable<String> v = validate(item, "select ?thing where { ?thing ?p\u00A0?o }");
        assertFalse(v.isValid());
        assertTrue(v.getErrors().getFirst().toString().contains("NO-BREAK SPACE"),
                v.getErrors().getFirst().toString());
    }

    // Other long literals are prose, and prose is not SPARQL.
    @Test
    void otherLongLiteralsAreNotCheckedAsSparql() throws Exception {
        TemplateContext context = queryTemplateContext();
        context.initStatements();
        LiteralTextfieldItem item = new LiteralTextfieldItem("value", DESCRIPTION_FIELD, true, context);
        assertTrue(validate(item, "This query returns the parts of a thing.").isValid());
    }

    @Test
    void emptySparqlIsLeftToTheRequiredFlag() throws Exception {
        TemplateContext context = queryTemplateContext();
        context.initStatements();
        LiteralTextfieldItem item = new LiteralTextfieldItem("value", SPARQL_FIELD, true, context);
        assertTrue(validate(item, "").isValid());
    }

}
