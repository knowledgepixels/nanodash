package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.PublishForm.FillMode;
import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.TemplateData;
import com.knowledgepixels.nanodash.template.TestFill;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.templates.Template;
import org.nanopub.templates.TemplateFillException;
import org.nanopub.vocabulary.NTEMPLATE;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Publishing is refused when a value is minted under one of the names a nanopublication keeps
 * for its own parts (issue #29) -- except where the nanopublication being superseded already
 * carried that name in its assertion, which is published and cannot be taken back. A legacy
 * template, whose template node is its own assertion graph, is republished that way. The
 * filler of nanopub-java decides this when it makes the nanopublication from the form (#936).
 */
class ReservedIdentifierPublishTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String TEMPLATE_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Res01";
    private static final IRI THING = vf.createIRI(TEMPLATE_URI + "/thing");
    private static final IRI NAME = vf.createIRI(TEMPLATE_URI + "/name");
    private static final String SOURCE_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Src01";

    private MockedStatic<TemplateData> templateDataMockedStatic;

    @BeforeEach
    void setUp() throws Exception {
        new WicketTester(new WicketApplication());
        templateDataMockedStatic = mockStatic(TemplateData.class);
        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(TEMPLATE_URI)).thenReturn(template());
    }

    @AfterEach
    void tearDown() {
        templateDataMockedStatic.close();
    }

    // ?thing rdfs:label ?name, where the thing is minted within the nanopublication.
    private static Template template() throws Exception {
        NanopubCreator creator = new NanopubCreator(TEMPLATE_URI);
        IRI node = creator.getAssertionUri();
        IRI st1 = vf.createIRI(TEMPLATE_URI + "/st1");
        creator.addAssertionStatement(node, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(node, RDFS.LABEL, vf.createLiteral("Reserved name test template"));
        creator.addAssertionStatement(node, NTEMPLATE.HAS_STATEMENT, st1);
        creator.addAssertionStatement(st1, RDF.SUBJECT, THING);
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st1, RDF.OBJECT, NAME);
        creator.addAssertionStatement(THING, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(THING, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
        creator.addAssertionStatement(THING, RDFS.LABEL, vf.createLiteral("the thing"));
        creator.addAssertionStatement(NAME, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(NAME, RDFS.LABEL, vf.createLiteral("the name"));
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        return new Template(creator.finalizeNanopub());
    }

    /**
     * A nanopublication that says something about the given IRI of its own.
     */
    private static Nanopub sourceUsing(String localName) throws Exception {
        NanopubCreator creator = new NanopubCreator(SOURCE_URI);
        IRI subject = vf.createIRI(SOURCE_URI + "/" + localName);
        creator.addAssertionStatement(subject, RDFS.LABEL, vf.createLiteral("an old template"));
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        return creator.finalizeNanopub();
    }

    // The form with the thing named "assertion".
    private static TemplateContext contextMintingAssertion(FillMode fillMode, Nanopub source) {
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, TEMPLATE_URI, "statement", (String) null);
        context.setFillMode(fillMode);
        context.setFillSource(source);
        context.getComponentModels().put(THING, Model.of("assertion"));
        context.getComponentModels().put(NAME, Model.of("a name"));
        return context;
    }

    private static void assertRefused(TemplateContext context) {
        TemplateFillException ex = assertThrows(TemplateFillException.class, () -> TestFill.fill(context));
        assertTrue(ex.getProblems().stream().anyMatch(p -> p.contains("'assertion' is what the nanopublication calls one of its own parts")),
                ex.getProblems().toString());
    }

    @Test
    void aFreshlyMintedPartNameIsRefused() {
        assertRefused(contextMintingAssertion(null, null));
    }

    @Test
    void aNameTheSupersededNanopublicationAlreadyCarriedIsLetThrough() throws Exception {
        Nanopub source = sourceUsing("assertion");
        Nanopub np = assertDoesNotThrow(() -> TestFill.fill(contextMintingAssertion(FillMode.SUPERSEDE, source)),
                "a new version keeps the shape of the one it supersedes");
        assertEquals(1, np.getAssertion().size());
    }

    @Test
    void supersedingSomethingElseDoesNotExcuseIt() throws Exception {
        assertRefused(contextMintingAssertion(FillMode.SUPERSEDE, sourceUsing("thing")));
    }

    @Test
    void derivingDoesNotExcuseIt() throws Exception {
        // A derived nanopublication is a new one, not a new version of its source.
        assertRefused(contextMintingAssertion(FillMode.DERIVE, sourceUsing("assertion")));
    }

}
