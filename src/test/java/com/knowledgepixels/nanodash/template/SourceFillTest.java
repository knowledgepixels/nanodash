package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.PublishForm.FillMode;
import com.knowledgepixels.nanodash.component.StatementItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
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
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Filling a form from an existing nanopublication with the reader of nanopub-java: each value
 * becomes the value its field starts with, under the name a link parameter would give it.
 */
class SourceFillTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String TEMPLATE_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_AbCdE";
    private static final IRI ST1 = vf.createIRI(TEMPLATE_URI + "/st1");
    private static final IRI ST2 = vf.createIRI(TEMPLATE_URI + "/st2");
    private static final IRI COMMENT = vf.createIRI(TEMPLATE_URI + "/comment");
    private static final IRI TITLE = vf.createIRI(TEMPLATE_URI + "/title");
    private static final IRI THING = vf.createIRI(TEMPLATE_URI + "/thing");
    private static final IRI SENTENCE = vf.createIRI(TEMPLATE_URI + "/sentence");
    private static final String SENTENCE_PREFIX = "http://example.org/sentences/";
    // Escaped with "%20" and "%2C", where the form would escape a space anew as "+".
    private static final String SENTENCE_IRI = SENTENCE_PREFIX + "A%20sentence%2C%20with%20a%20comma.";
    private static final IRI SUBJECT = vf.createIRI("http://example.com/subject");

    private static final String SOURCE_URI = "https://w3id.org/np/RAZyXwVuTsRqPoNmLkJiHgFeDcBa9876543210-_ZyXwV";

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

    @Test
    void aValueOfTheSourceStartsItsField() throws Exception {
        mockTemplate(false, false);
        TemplateContext context = fill(source(comment("first")), FillMode.USE, Map.of());

        assertEquals("first", valueOf(context, "comment"));
        assertEquals("a title", valueOf(context, "title"));
    }

    @Test
    void aParameterWinsOverTheSource() throws Exception {
        mockTemplate(false, false);
        TemplateContext context = fill(source(comment("first")), FillMode.SUPERSEDE, Map.of("comment", "mine"));

        assertEquals("mine", valueOf(context, "comment"));
        assertEquals("a title", valueOf(context, "title"), "the other fields are still filled from the source");
    }

    @Test
    void eachRepetitionOfAStatementGetsItsOwnFields() throws Exception {
        mockTemplate(true, false);
        TemplateContext context = fill(source(comment("first"), comment("second"), comment("third")), FillMode.USE, Map.of());

        StatementItem item = context.getStatementItems().get(0);
        assertEquals(3, item.getRepetitionCount());
        assertEquals(List.of("first", "second", "third"),
                List.of(valueOf(context, "comment"), valueOf(context, "comment__1"), valueOf(context, "comment__2")));
    }

    @Test
    void theLanguageOfALiteralComesWithIt() throws Exception {
        mockTemplate(false, true);
        Statement french = vf.createStatement(SUBJECT, RDFS.COMMENT, vf.createLiteral("maison", "fr"));
        TemplateContext context = fill(source(french), FillMode.USE, Map.of());

        assertEquals("maison", valueOf(context, "comment"));
        assertEquals("fr", valueOf(context, "comment" + TemplateContext.LANGUAGE_SUFFIX));
    }

    @Test
    void whatNoStatementTakesIsLeftOver() throws Exception {
        mockTemplate(false, false);
        Statement other = vf.createStatement(SUBJECT, RDFS.SEEALSO, vf.createIRI("http://example.com/other"));
        Nanopub source = source(comment("first"), other);
        TemplateContext context = newContext(FillMode.USE, source, Map.of());

        List<Statement> unused = new SourceFill(source, FillMode.USE, null).fillAssertion(context);

        assertEquals(1, unused.size());
        assertEquals(RDFS.SEEALSO, unused.get(0).getPredicate());
    }

    @Test
    void aNewVersionKeepsTheResourceTheSourceIntroduces() throws Exception {
        mockIntroducingTemplate();
        TemplateContext context = fill(introducingSource(), FillMode.SUPERSEDE, Map.of());

        assertEquals(SOURCE_URI + "/thing", valueOf(context, "thing"));
        assertEquals("the thing", valueOf(context, "title"));
    }

    @Test
    void aDerivedNanopublicationMintsTheResourceAnew() throws Exception {
        mockIntroducingTemplate();
        TemplateContext context = fill(introducingSource(), FillMode.DERIVE, Map.of());

        assertNotEquals(SOURCE_URI + "/thing", valueOf(context, "thing"));
        assertEquals("the thing", valueOf(context, "title"));
    }

    @Test
    void anEscapedResourceKeptInANewVersionIsShownAsTextAndMadeAsTheSourceSpellsIt() throws Exception {
        mockEscapingTemplate();
        TemplateContext context = fill(escapingSource(), FillMode.SUPERSEDE, Map.of());

        assertEquals("A sentence, with a comma.", valueOf(context, "sentence"));
        IRI made = vf.createIRI(SENTENCE_IRI);
        assertTrue(TestFill.fill(context).getAssertion().stream().anyMatch(st -> made.equals(st.getSubject()) || made.equals(st.getObject())),
                "the nanopublication the form makes must spell the IRI as the source does");
    }

    // ---------- fixtures ----------

    private static TemplateContext newContext(FillMode mode, Nanopub source, Map<String, String> params) {
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, TEMPLATE_URI, "statement", (String) null);
        context.setFillMode(mode);
        context.setFillSource(source);
        params.forEach(context::setParam);
        return context;
    }

    // As the form does it: the fill comes before the statements are made.
    private static TemplateContext fill(Nanopub source, FillMode mode, Map<String, String> params) {
        TemplateContext context = newContext(mode, source, params);
        new SourceFill(source, mode, null).fillAssertion(context);
        context.initStatements();
        context.finalizeStatements();
        return context;
    }

    private static String valueOf(TemplateContext context, String name) {
        Map<String, String> values = new TreeMap<>();
        for (Map.Entry<IRI, IModel<?>> e : context.getComponentModels().entrySet()) {
            Object value = e.getValue().getObject();
            values.put(Utils.getUriPostfix(e.getKey()), value == null ? null : value.toString());
        }
        return values.get(name);
    }

    private static Statement comment(String text) {
        return vf.createStatement(SUBJECT, RDFS.COMMENT, vf.createLiteral(text));
    }

    private static Nanopub source(Statement... comments) throws Exception {
        NanopubCreator creator = new NanopubCreator(SOURCE_URI);
        creator.setAssertionUri(SOURCE_URI + "/assertion");
        for (Statement st : comments) creator.addAssertionStatement(st);
        creator.addAssertionStatement(vf.createStatement(SUBJECT, RDFS.LABEL, vf.createLiteral("a title")));
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        return creator.finalizeNanopub();
    }

    private static Nanopub introducingSource() throws Exception {
        NanopubCreator creator = new NanopubCreator(SOURCE_URI);
        creator.setAssertionUri(SOURCE_URI + "/assertion");
        IRI thing = vf.createIRI(SOURCE_URI + "/thing");
        creator.addAssertionStatement(vf.createStatement(thing, RDFS.LABEL, vf.createLiteral("the thing")));
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), NPX.INTRODUCES, thing));
        return creator.finalizeNanopub();
    }

    private static Nanopub escapingSource() throws Exception {
        NanopubCreator creator = new NanopubCreator(SOURCE_URI);
        creator.setAssertionUri(SOURCE_URI + "/assertion");
        IRI sentence = vf.createIRI(SENTENCE_IRI);
        creator.addAssertionStatement(vf.createStatement(sentence, RDFS.LABEL, vf.createLiteral("the sentence")));
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), NPX.INTRODUCES, sentence));
        return creator.finalizeNanopub();
    }

    // SUBJECT rdfs:comment ?comment (repeatable or not, language selectable or not), and
    // SUBJECT rdfs:label ?title.
    private void mockTemplate(boolean repeatable, boolean languageSelectable) throws Exception {
        NanopubCreator creator = templateCreator();
        IRI node = creator.getAssertionUri();
        creator.addAssertionStatement(node, NTEMPLATE.HAS_STATEMENT, ST1);
        creator.addAssertionStatement(ST1, RDF.SUBJECT, SUBJECT);
        creator.addAssertionStatement(ST1, RDF.PREDICATE, RDFS.COMMENT);
        creator.addAssertionStatement(ST1, RDF.OBJECT, COMMENT);
        if (repeatable) creator.addAssertionStatement(ST1, RDF.TYPE, NTEMPLATE.REPEATABLE_STATEMENT);
        creator.addAssertionStatement(COMMENT, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        if (languageSelectable) creator.addAssertionStatement(COMMENT, RDF.TYPE, NTEMPLATE.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(COMMENT, RDFS.LABEL, vf.createLiteral("comment"));
        addTitleStatement(creator, SUBJECT);
        register(creator);
    }

    // sub:thing, a local resource the nanopublication introduces, rdfs:label ?title.
    private void mockIntroducingTemplate() throws Exception {
        NanopubCreator creator = templateCreator();
        creator.addAssertionStatement(THING, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
        creator.addAssertionStatement(THING, RDF.TYPE, NTEMPLATE.INTRODUCED_RESOURCE);
        creator.addAssertionStatement(THING, RDFS.LABEL, vf.createLiteral("the thing"));
        addTitleStatement(creator, THING);
        register(creator);
    }

    // ?sentence, an IRI made from its text by escaping it and introduced, rdfs:label ?title.
    private void mockEscapingTemplate() throws Exception {
        NanopubCreator creator = templateCreator();
        creator.addAssertionStatement(SENTENCE, RDF.TYPE, NTEMPLATE.AUTO_ESCAPE_URI_PLACEHOLDER);
        creator.addAssertionStatement(SENTENCE, RDF.TYPE, NTEMPLATE.INTRODUCED_RESOURCE);
        creator.addAssertionStatement(SENTENCE, NTEMPLATE.HAS_PREFIX, vf.createLiteral(SENTENCE_PREFIX));
        creator.addAssertionStatement(SENTENCE, RDFS.LABEL, vf.createLiteral("sentence"));
        addTitleStatement(creator, SENTENCE);
        register(creator);
    }

    private static NanopubCreator templateCreator() throws Exception {
        NanopubCreator creator = new NanopubCreator(TEMPLATE_URI);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        IRI node = creator.getAssertionUri();
        creator.addAssertionStatement(node, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(node, RDFS.LABEL, vf.createLiteral("Source fill test template"));
        return creator;
    }

    private static void addTitleStatement(NanopubCreator creator, Value subject) throws Exception {
        creator.addAssertionStatement(creator.getAssertionUri(), NTEMPLATE.HAS_STATEMENT, ST2);
        creator.addAssertionStatement(ST2, RDF.SUBJECT, subject);
        creator.addAssertionStatement(ST2, RDF.PREDICATE, RDFS.LABEL);
        creator.addAssertionStatement(ST2, RDF.OBJECT, TITLE);
        creator.addAssertionStatement(TITLE, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(TITLE, RDFS.LABEL, vf.createLiteral("title"));
    }

    private void register(NanopubCreator creator) throws Exception {
        Template template = new Template(creator.finalizeNanopub());
        TemplateData templateDataMock = mock(TemplateData.class);
        templateDataMockedStatic.when(TemplateData::get).thenReturn(templateDataMock);
        when(templateDataMock.getTemplate(TEMPLATE_URI)).thenReturn(template);
    }

}
