package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.StatementItem;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.MockedStatic;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Fill/unification tests for member-level optionality inside grouped statements
 * (docs/optional-statements-in-groups.md): a group matches a nanopub that omits
 * an optional member's triple, the skipped member is tracked for row hiding,
 * repetitions handle the optional member independently, and matching stays
 * terminating and evidence-based (at least one statement consumed).
 */
public class OptionalGroupMemberFillTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String NP_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI GROUP = vf.createIRI(NP_URI + "/group");
    private static final IRI ST1 = vf.createIRI(NP_URI + "/st1");
    private static final IRI ST2 = vf.createIRI(NP_URI + "/st2");
    private static final IRI THING = vf.createIRI(NP_URI + "/thing");
    private static final IRI NAME = vf.createIRI(NP_URI + "/name");
    private static final IRI SOME_CLASS = vf.createIRI("http://example.com/SomeClass");

    private static final IRI THING1 = vf.createIRI("http://example.com/thing1");
    private static final IRI THING2 = vf.createIRI("http://example.com/thing2");

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
     * Builds the mixed-group template — st1 (required: thing a SomeClass), st2
     * (optional: thing rdfs:label name) — optionally repeatable, and registers it
     * with the mocked TemplateData.
     */
    private void mockTemplate(boolean repeatable) throws Exception {
        NanopubCreator creator = new NanopubCreator(NP_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI templateNode = creator.getAssertionUri();
        creator.addAssertionStatement(templateNode, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(templateNode, RDFS.LABEL, vf.createLiteral("Group test template"));
        creator.addAssertionStatement(templateNode, NTEMPLATE.HAS_STATEMENT, GROUP);
        creator.addAssertionStatement(GROUP, RDF.TYPE, NTEMPLATE.GROUPED_STATEMENT);
        if (repeatable) {
            creator.addAssertionStatement(GROUP, RDF.TYPE, NTEMPLATE.REPEATABLE_STATEMENT);
        }
        creator.addAssertionStatement(GROUP, NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(GROUP, NTEMPLATE.HAS_STATEMENT, ST2);
        creator.addAssertionStatement(ST1, RDF.SUBJECT, THING);
        creator.addAssertionStatement(ST1, RDF.PREDICATE, RDF.TYPE);
        creator.addAssertionStatement(ST1, RDF.OBJECT, SOME_CLASS);
        creator.addAssertionStatement(ST2, RDF.TYPE, NTEMPLATE.OPTIONAL_STATEMENT);
        creator.addAssertionStatement(ST2, RDF.SUBJECT, THING);
        creator.addAssertionStatement(ST2, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(ST2, RDF.OBJECT, NAME);
        creator.addAssertionStatement(THING, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(THING, RDFS.LABEL, vf.createLiteral("thing"));
        creator.addAssertionStatement(NAME, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(NAME, RDFS.LABEL, vf.createLiteral("name"));
        Template template = new Template(creator.finalizeNanopub());

        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(NP_URI)).thenReturn(template);
    }

    /**
     * Builds a data nanopub with the given assertion statements.
     */
    private static Nanopub dataNanopub(Statement... assertionStatements) throws Exception {
        NanopubCreator creator = new NanopubCreator("http://purl.org/nanopub/temp/data/");
        for (Statement st : assertionStatements) {
            creator.addAssertionStatement(st);
        }
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        return creator.finalizeNanopub();
    }

    /**
     * Mirrors the viewer flow (NanopubItem.populateStatementItemList): read-only
     * context built from the template, fed the data nanopub's assertion triples.
     */
    private TemplateContext fill(Nanopub dataNp, ValueFiller filler) {
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, NP_URI, "statement", dataNp);
        context.initStatements();
        filler.fill(context);
        return context;
    }

    @SuppressWarnings("unchecked")
    private static Set<IRI> getUnmatchedParts(StatementItem si) throws Exception {
        Field rgsField = StatementItem.class.getDeclaredField("repetitionGroups");
        rgsField.setAccessible(true);
        Object rg = ((List<Object>) rgsField.get(si)).get(0);
        Field upField = rg.getClass().getDeclaredField("unmatchedParts");
        upField.setAccessible(true);
        return (Set<IRI>) upField.get(rg);
    }

    @Test
    void nanopubOmittingOptionalTripleMatches() throws Exception {
        mockTemplate(false);
        Nanopub dataNp = dataNanopub(vf.createStatement(THING1, RDF.TYPE, SOME_CLASS));
        ValueFiller filler = new ValueFiller(dataNp, ContextType.ASSERTION, false);
        TemplateContext context = fill(dataNp, filler);

        StatementItem si = context.getStatementItems().get(0);
        assertTrue(si.isMatched(), "group must match despite the missing optional triple");
        assertTrue(filler.getUnusedStatements().isEmpty(),
                "all statements must be consumed, but these stayed unused:\n" + filler.getUnusedStatements());
        assertEquals(Set.of(ST2), getUnmatchedParts(si), "the skipped optional member must be tracked for row hiding");
    }

    @Test
    void nanopubWithOptionalTripleMatchesFully() throws Exception {
        mockTemplate(false);
        Nanopub dataNp = dataNanopub(
                vf.createStatement(THING1, RDF.TYPE, SOME_CLASS),
                vf.createStatement(THING1, RDFS.LABEL, vf.createLiteral("thing one")));
        ValueFiller filler = new ValueFiller(dataNp, ContextType.ASSERTION, false);
        TemplateContext context = fill(dataNp, filler);

        StatementItem si = context.getStatementItems().get(0);
        assertTrue(si.isMatched());
        assertTrue(filler.getUnusedStatements().isEmpty(),
                "all statements must be consumed, but these stayed unused:\n" + filler.getUnusedStatements());
        assertTrue(getUnmatchedParts(si).isEmpty(), "nothing was skipped, so no row may be hidden");
    }

    @Test
    void optionalTripleAloneDoesNotMatchGroup() throws Exception {
        mockTemplate(false);
        Nanopub dataNp = dataNanopub(vf.createStatement(THING1, RDFS.LABEL, vf.createLiteral("thing one")));
        ValueFiller filler = new ValueFiller(dataNp, ContextType.ASSERTION, false);
        TemplateContext context = fill(dataNp, filler);

        StatementItem si = context.getStatementItems().get(0);
        assertFalse(si.isMatched(), "required member has no candidate, so the group must not match");
        assertEquals(1, filler.getUnusedStatements().size(), "the label triple must stay unused");
    }

    @Test
    @Timeout(10)
    void repeatableGroupHandlesOptionalMemberPerRepetition() throws Exception {
        mockTemplate(true);
        Nanopub dataNp = dataNanopub(
                vf.createStatement(THING1, RDF.TYPE, SOME_CLASS),
                vf.createStatement(THING1, RDFS.LABEL, vf.createLiteral("thing one")),
                vf.createStatement(THING2, RDF.TYPE, SOME_CLASS));
        ValueFiller filler = new ValueFiller(dataNp, ContextType.ASSERTION, false);
        TemplateContext context = fill(dataNp, filler);

        StatementItem si = context.getStatementItems().get(0);
        assertTrue(si.isMatched());
        assertEquals(2, si.getRepetitionCount(),
                "one repetition with the optional triple, one without");
        assertTrue(filler.getUnusedStatements().isEmpty(),
                "all statements must be consumed, but these stayed unused:\n" + filler.getUnusedStatements());
    }

    @Test
    @Timeout(10)
    void repeatableGroupTerminatesWhenNothingIsLeft() throws Exception {
        mockTemplate(true);
        Nanopub dataNp = dataNanopub(vf.createStatement(THING1, RDF.TYPE, SOME_CLASS));
        ValueFiller filler = new ValueFiller(dataNp, ContextType.ASSERTION, false);
        TemplateContext context = fill(dataNp, filler);

        StatementItem si = context.getStatementItems().get(0);
        assertTrue(si.isMatched());
        // The all-consumed statement list must not allow further vacuous "matches"
        // (every member skipped): the repetition loop has to stop.
        assertTrue(filler.getUnusedStatements().isEmpty());
    }

}
