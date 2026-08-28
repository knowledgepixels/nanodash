package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.StatementItem;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests the auto-numbering of local resources in repeatable statement groups (issue #651).
 *
 * <p>A local resource that is scoped to a single repeatable statement gets a fresh instance per
 * repetition, so its label is numbered to keep the repetitions distinguishable. A local resource
 * that also occurs in other statements — typically the introduced resource that the whole
 * template is about — denotes the same thing in every repetition and must stay unnumbered.</p>
 */
public class RepeatedLocalResourceLabelTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String SHARED_NP = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Shar1";
    private static final String SCOPED_NP = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Scop1";

    private WicketTester tester;
    private MockedStatic<TemplateData> templateDataMockedStatic;

    @BeforeEach
    void setUp() {
        tester = new WicketTester(new WicketApplication());
        templateDataMockedStatic = mockStatic(TemplateData.class);
    }

    @AfterEach
    void tearDown() {
        templateDataMockedStatic.close();
    }

    /**
     * A template whose introduced resource ("this nanosuggestion") is the subject of a plain
     * statement and of a repeatable one, as in issue #651.
     */
    private TemplateContext sharedResourceContext() throws Exception {
        IRI st1 = vf.createIRI(SHARED_NP + "/st1");
        IRI st2 = vf.createIRI(SHARED_NP + "/st2");
        IRI suggestion = vf.createIRI(SHARED_NP + "/suggestion");
        IRI space = vf.createIRI(SHARED_NP + "/space");
        NanopubCreator creator = newTemplate(SHARED_NP, "Shared resource template", st1, st2);
        creator.addAssertionStatement(st1, RDF.SUBJECT, suggestion);
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st1, RDF.OBJECT, vf.createIRI(SHARED_NP + "/title"));
        creator.addAssertionStatement(vf.createIRI(SHARED_NP + "/title"), RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(vf.createIRI(SHARED_NP + "/title"), RDFS.LABEL, vf.createLiteral("the title"));
        creator.addAssertionStatement(st2, RDF.TYPE, NTEMPLATE.REPEATABLE_STATEMENT);
        creator.addAssertionStatement(st2, RDF.SUBJECT, suggestion);
        creator.addAssertionStatement(st2, RDF.PREDICATE, RDFS.SEEALSO);
        creator.addAssertionStatement(st2, RDF.OBJECT, space);
        creator.addAssertionStatement(suggestion, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
        creator.addAssertionStatement(suggestion, RDF.TYPE, NTEMPLATE.INTRODUCED_RESOURCE);
        creator.addAssertionStatement(suggestion, RDFS.LABEL, vf.createLiteral("this nanosuggestion"));
        creator.addAssertionStatement(space, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(space, RDFS.LABEL, vf.createLiteral("a space"));
        return contextFor(SHARED_NP, creator);
    }

    /**
     * A template whose local resource occurs only within the repeatable statement, so each
     * repetition mints its own instance.
     */
    private TemplateContext scopedResourceContext() throws Exception {
        IRI st1 = vf.createIRI(SCOPED_NP + "/st1");
        IRI alias = vf.createIRI(SCOPED_NP + "/alias");
        IRI name = vf.createIRI(SCOPED_NP + "/name");
        NanopubCreator creator = newTemplate(SCOPED_NP, "Scoped resource template", st1);
        creator.addAssertionStatement(st1, RDF.TYPE, NTEMPLATE.REPEATABLE_STATEMENT);
        creator.addAssertionStatement(st1, RDF.SUBJECT, alias);
        creator.addAssertionStatement(st1, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(st1, RDF.OBJECT, name);
        creator.addAssertionStatement(alias, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
        creator.addAssertionStatement(alias, RDFS.LABEL, vf.createLiteral("a context-specific alias"));
        creator.addAssertionStatement(name, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(name, RDFS.LABEL, vf.createLiteral("the name"));
        return contextFor(SCOPED_NP, creator);
    }

    private static NanopubCreator newTemplate(String npUri, String label, IRI... statements) throws Exception {
        NanopubCreator creator = new NanopubCreator(npUri);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral(label));
        for (IRI st : statements) {
            creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, st);
        }
        return creator;
    }

    private TemplateContext contextFor(String npUri, NanopubCreator creator) throws Exception {
        Template template = new Template(creator.finalizeNanopub());
        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(npUri)).thenReturn(template);
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, npUri, "statement", (String) null);
        context.initStatements();
        return context;
    }

    /**
     * Renders the given statement item and returns the labels of its resource links, in order.
     */
    private List<String> linkLabels(StatementItem si) {
        tester.startComponentInPage(si);
        List<String> labels = new ArrayList<>();
        si.visitChildren(ExternalLink.class, (IVisitor<Component, Void>) (c, visit) -> {
            Object body = ((ExternalLink) c).getBody().getObject();
            if (body != null) labels.add(body.toString());
        });
        return labels;
    }

    @Test
    void sharedIntroducedResourceIsNotNumbered() throws Exception {
        TemplateContext context = sharedResourceContext();
        StatementItem repeatable = context.getStatementItems().get(1);
        repeatable.addRepetitionGroup();
        assertEquals(2, repeatable.getRepetitionCount());

        List<String> labels = linkLabels(repeatable);
        assertEquals(List.of("This nanosuggestion", "This nanosuggestion"), subjectLabels(labels, "This nanosuggestion"),
                "the introduced resource is the same in every repetition and must not be numbered");
    }

    @Test
    void resourceScopedToRepeatableStatementIsNumbered() throws Exception {
        TemplateContext context = scopedResourceContext();
        StatementItem repeatable = context.getStatementItems().get(0);
        repeatable.addRepetitionGroup();
        assertEquals(2, repeatable.getRepetitionCount());

        List<String> labels = linkLabels(repeatable);
        assertEquals(List.of("A context-specific alias 1", "A context-specific alias 2"),
                subjectLabels(labels, "A context-specific alias"),
                "a per-repetition local resource stays distinguishable by its index");
    }

    private static List<String> subjectLabels(List<String> labels, String prefix) {
        List<String> matching = new ArrayList<>();
        for (String l : labels) {
            if (l.startsWith(prefix)) matching.add(l);
        }
        return matching;
    }

}
