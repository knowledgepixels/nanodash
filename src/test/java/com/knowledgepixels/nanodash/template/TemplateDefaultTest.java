package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.component.PublishForm;
import com.knowledgepixels.nanodash.page.PublishPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.templates.RawValue;
import org.nanopub.vocabulary.NTEMPLATE;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A field starts out with the default value the template gives its placeholder, written as the
 * library writes that value for the field, and a field left with its default is left to the filler
 * to fill in (#936).
 */
class TemplateDefaultTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String TEMPLATE_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Def01";
    private static final IRI SUBJECT = vf.createIRI("http://example.com/subject");
    private static final IRI PAGE = iri("page");
    private static final IRI PAPER = iri("paper");
    private static final IRI THING = iri("thing");
    private static final IRI AUTHOR = iri("author");
    private static final IRI CODE = iri("code");
    private static final IRI STATUS = iri("status");
    private static final IRI CREATOR = vf.createIRI("https://orcid.org/0000-0000-0000-0001");

    private WicketTester tester;

    private static IRI iri(String name) {
        return vf.createIRI(TEMPLATE_URI + "/" + name);
    }

    @BeforeEach
    void setUp() throws Exception {
        tester = new WicketTester(new WicketApplication());
        NanopubCreator creator = new NanopubCreator(TEMPLATE_URI);
        IRI node = creator.getAssertionUri();
        creator.addAssertionStatement(node, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(node, RDFS.LABEL, vf.createLiteral("Default value test template"));
        int n = 0;
        for (IRI placeholder : List.of(PAGE, PAPER, AUTHOR, CODE, STATUS)) {
            IRI st = iri("st" + (++n));
            creator.addAssertionStatement(node, NTEMPLATE.HAS_STATEMENT, st);
            creator.addAssertionStatement(st, RDF.SUBJECT, THING);
            creator.addAssertionStatement(st, RDF.PREDICATE, vf.createIRI("http://example.com/p" + n));
            creator.addAssertionStatement(st, RDF.OBJECT, placeholder);
            creator.addAssertionStatement(placeholder, RDFS.LABEL, vf.createLiteral("the " + placeholder.getLocalName()));
        }
        creator.addAssertionStatement(THING, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(THING, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
        creator.addAssertionStatement(THING, NTEMPLATE.HAS_DEFAULT_VALUE, iri("item"));
        creator.addAssertionStatement(PAGE, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(PAGE, NTEMPLATE.HAS_DEFAULT_VALUE, vf.createIRI("https://example.com/start"));
        creator.addAssertionStatement(PAPER, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(PAPER, NTEMPLATE.HAS_PREFIX, vf.createLiteral("https://doi.org/"));
        creator.addAssertionStatement(PAPER, NTEMPLATE.HAS_DEFAULT_VALUE, vf.createIRI("https://doi.org/10.1234/abc"));
        creator.addAssertionStatement(AUTHOR, RDF.TYPE, NTEMPLATE.AGENT_PLACEHOLDER);
        creator.addAssertionStatement(AUTHOR, NTEMPLATE.HAS_DEFAULT_VALUE, NTEMPLATE.CREATOR_PLACEHOLDER);
        creator.addAssertionStatement(CODE, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(CODE, NTEMPLATE.HAS_REGEX, vf.createLiteral("[0-9]+"));
        creator.addAssertionStatement(CODE, NTEMPLATE.HAS_DEFAULT_VALUE, vf.createLiteral("none"));
        creator.addAssertionStatement(STATUS, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(STATUS, NTEMPLATE.HAS_DEFAULT_VALUE, vf.createLiteral("checked"));
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        TemplateData.get().registerTemplate(creator.finalizeNanopub());
    }

    @AfterEach
    void tearDown() {
        tester.destroy();
    }

    private static TemplateContext context(IRI creator) {
        TemplateContext context = new TemplateContext(ContextType.ASSERTION, TEMPLATE_URI, "statement", (String) null);
        // The form keeps the user as the value of nt:CREATOR.
        context.getComponentModels().remove(NTEMPLATE.CREATOR_PLACEHOLDER);
        if (creator != null) context.getComponentModels().put(NTEMPLATE.CREATOR_PLACEHOLDER, Model.of(creator.stringValue()));
        return context;
    }

    private static String defaultText(TemplateContext context, IRI placeholder) {
        RawValue raw = context.getDefaultRaw(placeholder);
        return raw == null ? null : raw.text();
    }

    @Test
    void theDefaultIsWrittenAsItIsEntered() {
        TemplateContext context = context(CREATOR);
        assertEquals("https://example.com/start", defaultText(context, PAGE));
        assertEquals("10.1234/abc", defaultText(context, PAPER), "the part after the prefix");
        assertEquals("item", defaultText(context, THING), "an IRI within the template is the name minted within the nanopublication");
        assertEquals("checked", defaultText(context, STATUS));
    }

    @Test
    void theCreatorAsDefaultIsTheUser() {
        assertEquals(CREATOR.stringValue(), defaultText(context(CREATOR), AUTHOR));
        assertNull(defaultText(context(null), AUTHOR), "nobody is logged in");
    }

    @Test
    void aDefaultTheFieldCannotTakeIsNone() {
        assertNull(defaultText(context(CREATOR), CODE));
    }

    @Test
    void aFieldWithItsDefaultIsLeftToTheFiller() {
        TemplateContext context = context(CREATOR);
        assertTrue(FormRequest.isDefault(context, PAPER, "10.1234/abc", null));
        assertTrue(FormRequest.isDefault(context, PAPER, "https://doi.org/10.1234/abc", null), "the same IRI written in full");
        assertFalse(FormRequest.isDefault(context, PAPER, "10.1234/other", null));
        assertTrue(FormRequest.isDefault(context, THING, "item", null));
        assertTrue(FormRequest.isDefault(context, STATUS, "checked", null));
        assertFalse(FormRequest.isDefault(context, STATUS, "", null), "a cleared field has no value");
        assertFalse(FormRequest.isDefault(context, CODE, "none", null), "a default that cannot stand there is not one");
    }

    @Test
    void theFormStartsWithTheDefaultsAndMakesThemAgain() throws Exception {
        // The user the form is filled in by, whom nt:CREATOR stands for.
        Field user = NanodashSession.class.getDeclaredField("userIri");
        user.setAccessible(true);
        user.set(NanodashSession.get(), CREATOR);
        PublishForm form = new PublishForm("panel", new PageParameters().add("template", TEMPLATE_URI), PublishPage.class, null);
        tester.startComponentInPage(form);
        Field field = PublishForm.class.getDeclaredField("assertionContext");
        field.setAccessible(true);
        TemplateContext context = (TemplateContext) field.get(form);

        assertEquals("https://example.com/start", model(context, PAGE));
        assertEquals("10.1234/abc", model(context, PAPER));
        assertEquals("item", model(context, THING));
        assertEquals("checked", model(context, STATUS));
        assertEquals(CREATOR.stringValue(), model(context, AUTHOR));
        assertEquals("", model(context, CODE));

        @SuppressWarnings("unchecked")
        IModel<String> code = (IModel<String>) context.getComponentModels().get(CODE);
        code.setObject("42");
        Nanopub np = TestFill.fill(context);
        String ns = np.getUri().stringValue();
        assertTrue(has(np, vf.createIRI(ns + "item"), vf.createIRI("http://example.com/p1"), vf.createIRI("https://example.com/start")), np.getAssertion().toString());
        assertTrue(has(np, vf.createIRI(ns + "item"), vf.createIRI("http://example.com/p2"), vf.createIRI("https://doi.org/10.1234/abc")), np.getAssertion().toString());
        assertTrue(has(np, vf.createIRI(ns + "item"), vf.createIRI("http://example.com/p3"), CREATOR), np.getAssertion().toString());
        assertTrue(has(np, vf.createIRI(ns + "item"), vf.createIRI("http://example.com/p5"), vf.createLiteral("checked")), np.getAssertion().toString());
    }

    private static Object model(TemplateContext context, IRI placeholder) {
        IModel<?> model = context.getComponentModels().get(placeholder);
        return model == null ? null : model.getObject();
    }

    private static boolean has(Nanopub np, IRI subject, IRI predicate, org.eclipse.rdf4j.model.Value object) {
        return np.getAssertion().stream().anyMatch(st -> st.getSubject().equals(subject) && st.getPredicate().equals(predicate)
                && st.getObject().equals(object));
    }

}
