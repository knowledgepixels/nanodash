package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubCreator;
import org.nanopub.templates.FillRequest;
import org.nanopub.templates.TemplateFiller;
import org.nanopub.trusty.MakeTrustyNanopub;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.NTEMPLATE;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the nanopublication the publishing form makes, with the filler of nanopub-java from what
 * is filled in on the form (#936). The default provenance and publication info templates are
 * retrieved from the network, as the form does.
 */
class PublishFormBuildTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String TEMPLATE = "https://w3id.org/np/RAFillGuardFillGuardFillGuardFillGuardFillGua";
    private static final IRI ME = vf.createIRI("https://orcid.org/0000-0002-1267-0234");

    private WicketTester tester;

    @BeforeEach
    void setUp() throws Exception {
        tester = new WicketTester(new WicketApplication());
        registerTemplate();
        Field f = NanodashSession.class.getDeclaredField("userIri");
        f.setAccessible(true);
        f.set(NanodashSession.get(), ME);
    }

    // A template for a thing minted in the nanopublication, which introduces it:
    //   ?thing rdfs:label ?name .
    //   ?thing rdfs:comment ?comment .     (optional; the language is chosen)
    //   ?thing dct:subject ?keyword .      (optional, repeatable)
    private static void registerTemplate() throws Exception {
        NanopubCreator creator = new NanopubCreator(TEMPLATE);
        IRI node = creator.getAssertionUri();
        IRI thing = vf.createIRI(TEMPLATE + "/thing");
        IRI name = vf.createIRI(TEMPLATE + "/name");
        IRI comment = vf.createIRI(TEMPLATE + "/comment");
        IRI keyword = vf.createIRI(TEMPLATE + "/keyword");
        creator.addAssertionStatement(node, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(node, RDFS.LABEL, vf.createLiteral("Fill guard test template"));
        creator.addAssertionStatement(node, NTEMPLATE.HAS_NANOPUB_LABEL_PATTERN, vf.createLiteral("Thing: ${name}"));
        addStatement(creator, node, "st1", thing, RDFS.LABEL, name);
        addStatement(creator, node, "st2", thing, RDFS.COMMENT, comment, NTEMPLATE.OPTIONAL_STATEMENT);
        addStatement(creator, node, "st3", thing, DCTERMS.SUBJECT, keyword, NTEMPLATE.OPTIONAL_STATEMENT, NTEMPLATE.REPEATABLE_STATEMENT);
        // An optional group whose status has a default, which the form fills in: left
        // untouched, the group is left out, default and all.
        IRI group = vf.createIRI(TEMPLATE + "/st4");
        IRI status = vf.createIRI(TEMPLATE + "/status");
        IRI link = vf.createIRI(TEMPLATE + "/link");
        creator.addAssertionStatement(node, NTEMPLATE.HAS_STATEMENT, group);
        creator.addAssertionStatement(group, RDF.TYPE, NTEMPLATE.GROUPED_STATEMENT);
        creator.addAssertionStatement(group, RDF.TYPE, NTEMPLATE.OPTIONAL_STATEMENT);
        IRI st4a = vf.createIRI(TEMPLATE + "/st4a");
        IRI st4b = vf.createIRI(TEMPLATE + "/st4b");
        creator.addAssertionStatement(group, NTEMPLATE.HAS_STATEMENT, st4a);
        creator.addAssertionStatement(group, NTEMPLATE.HAS_STATEMENT, st4b);
        creator.addAssertionStatement(st4a, RDF.SUBJECT, thing);
        creator.addAssertionStatement(st4a, RDF.PREDICATE, DCTERMS.SOURCE);
        creator.addAssertionStatement(st4a, RDF.OBJECT, link);
        creator.addAssertionStatement(st4b, RDF.SUBJECT, link);
        creator.addAssertionStatement(st4b, RDF.PREDICATE, RDFS.COMMENT);
        creator.addAssertionStatement(st4b, RDF.OBJECT, status);
        creator.addAssertionStatement(link, RDF.TYPE, NTEMPLATE.EXTERNAL_URI_PLACEHOLDER);
        creator.addAssertionStatement(link, RDFS.LABEL, vf.createLiteral("a source"));
        creator.addAssertionStatement(status, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(status, RDFS.LABEL, vf.createLiteral("its status"));
        creator.addAssertionStatement(status, NTEMPLATE.HAS_DEFAULT_VALUE, vf.createLiteral("checked"));
        creator.addAssertionStatement(thing, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(thing, RDF.TYPE, NTEMPLATE.LOCAL_RESOURCE);
        creator.addAssertionStatement(thing, RDF.TYPE, NTEMPLATE.INTRODUCED_RESOURCE);
        creator.addAssertionStatement(thing, RDFS.LABEL, vf.createLiteral("the thing"));
        creator.addAssertionStatement(name, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(name, RDFS.LABEL, vf.createLiteral("the name"));
        creator.addAssertionStatement(comment, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(comment, RDF.TYPE, NTEMPLATE.LANGUAGE_TAGGED_LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(comment, RDFS.LABEL, vf.createLiteral("a comment"));
        creator.addAssertionStatement(keyword, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(keyword, RDFS.LABEL, vf.createLiteral("a keyword"));
        creator.addProvenanceStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri());
        creator.addPubinfoStatement(RDFS.SEEALSO, creator.getNanopubUri());
        Nanopub np = creator.finalizeNanopub();
        assertNotNull(TemplateData.get().registerTemplate(np));
        // For the library, which retrieves templates through Nanodash's cache:
        Utils.cacheNanopub(np);
    }

    private static void addStatement(NanopubCreator creator, IRI node, String id, IRI subject, IRI predicate, IRI object, IRI... types) throws Exception {
        IRI st = vf.createIRI(TEMPLATE + "/" + id);
        creator.addAssertionStatement(node, NTEMPLATE.HAS_STATEMENT, st);
        creator.addAssertionStatement(st, RDF.SUBJECT, subject);
        creator.addAssertionStatement(st, RDF.PREDICATE, predicate);
        creator.addAssertionStatement(st, RDF.OBJECT, object);
        for (IRI type : types) creator.addAssertionStatement(st, RDF.TYPE, type);
    }

    private PublishForm openForm(PageParameters params) {
        PublishForm form = new PublishForm("panel", params.add("template", TEMPLATE), PublishPage.class, null);
        tester.startComponentInPage(form);
        return form;
    }

    private static Nanopub createNanopub(PublishForm form) throws Exception {
        Method createNanopub = PublishForm.class.getDeclaredMethod("createNanopub");
        createNanopub.setAccessible(true);
        return (Nanopub) createNanopub.invoke(form);
    }

    private static boolean has(Collection<Statement> statements, IRI subject, IRI predicate, Value object) {
        return statements.stream().anyMatch(st -> st.getSubject().equals(subject) && st.getPredicate().equals(predicate) && st.getObject().equals(object));
    }

    private static boolean has(Collection<Statement> statements, IRI predicate, String object) {
        return statements.stream().anyMatch(st -> st.getPredicate().equals(predicate) && st.getObject().stringValue().equals(object));
    }

    @Test
    void newNanopublication() throws Exception {
        PublishForm form = openForm(new PageParameters().add("param_thing", "apple").add("param_name", "Apple")
                .add("param_comment", "Ein Apfel").add("param_comment__lang", "de")
                .add("param_keyword", "red").add("param_keyword__1", "green"));
        FillRequest request = form.toFillRequest();
        assertEquals("apple", request.getValues().get("thing").text());
        assertEquals("de", request.getValues().get("comment").languageTag());
        assertEquals("green", request.getValues().get("keyword__1").text());

        Nanopub np = createNanopub(form);
        String thing = np.getUri().stringValue().replaceFirst("[/#]?$", "/") + "apple";
        assertTrue(has(np.getAssertion(), vf.createIRI(thing), RDFS.LABEL, vf.createLiteral("Apple")), np.getAssertion().toString());
        assertTrue(has(np.getAssertion(), vf.createIRI(thing), RDFS.COMMENT, vf.createLiteral("Ein Apfel", "de")), np.getAssertion().toString());
        assertTrue(has(np.getAssertion(), DCTERMS.SUBJECT, "red"), np.getAssertion().toString());
        assertTrue(has(np.getAssertion(), DCTERMS.SUBJECT, "green"), np.getAssertion().toString());
        assertTrue(has(np.getPubinfo(), NPX.INTRODUCES, thing), np.getPubinfo().toString());
        assertTrue(has(np.getPubinfo(), RDFS.LABEL, "Thing: Apple"), np.getPubinfo().toString());
        assertTrue(has(np.getPubinfo(), NTEMPLATE.WAS_CREATED_FROM_TEMPLATE, TEMPLATE), np.getPubinfo().toString());
        assertTrue(has(np.getPubinfo(), DCTERMS.CREATOR, ME.stringValue()), np.getPubinfo().toString());

        // Signing gives it its place on the network, with what it mints below it.
        Nanopub trusty = MakeTrustyNanopub.transform(np);
        String uri = trusty.getUri().stringValue();
        assertTrue(uri.matches("https://w3id\\.org/np/RA[A-Za-z0-9_-]{43}"), uri);
        assertTrue(has(trusty.getPubinfo(), NPX.INTRODUCES, uri + "/apple"), trusty.getPubinfo().toString());
    }

    @Test
    void newNanopublicationWithoutTheOptionalStatements() throws Exception {
        PublishForm form = openForm(new PageParameters().add("param_thing", "apple").add("param_name", "Apple"));
        // The default the form filled in is not passed on as a value given, which the library
        // would refuse to leave out with its statement.
        assertFalse(form.toFillRequest().getValues().has("status"), form.toFillRequest().toParams().toString());
        Nanopub np = createNanopub(form);
        assertEquals(1, np.getAssertion().size(), np.getAssertion().toString());
    }

    @Test
    void defaultInAGroupThatIsFilledIn() throws Exception {
        PublishForm form = openForm(new PageParameters().add("param_thing", "apple").add("param_name", "Apple")
                .add("param_link", "https://example.org/apple-source"));
        Nanopub np = createNanopub(form);
        assertTrue(np.getAssertion().stream().anyMatch(st -> st.getPredicate().equals(RDFS.COMMENT)
                && st.getObject().stringValue().equals("checked")), np.getAssertion().toString());
    }

    @Test
    void newVersion() throws Exception {
        // The version before, as the library makes it, made trusty and cached so that the
        // form finds it without the network.
        Nanopub before = MakeTrustyNanopub.transform(new TemplateFiller(TemplateData.RESOLVER).fill(new FillRequest(TEMPLATE)
                .set("thing", "apple").set("name", "Apple").set("keyword", "red").setCreator(ME)));
        Utils.cacheNanopub(before);

        PublishForm form = openForm(new PageParameters().add("supersede", before.getUri().stringValue()).add("param_name", "Apple tree"));
        // The new version keeps the IRI of what it introduces.
        Nanopub np = createNanopub(form);
        assertTrue(has(np.getAssertion(), RDFS.LABEL, "Apple tree"), np.getAssertion().toString());
        assertTrue(has(np.getAssertion(), DCTERMS.SUBJECT, "red"), "the rest is filled from the version before: " + np.getAssertion());
        assertTrue(has(np.getPubinfo(), NPX.SUPERSEDES, before.getUri().stringValue()), np.getPubinfo().toString());
        assertTrue(np.getPubinfo().stream().anyMatch(st -> st.getPredicate().equals(NPX.INTRODUCES)
                && st.getObject().stringValue().equals(before.getUri().stringValue() + "/apple")), np.getPubinfo().toString());
    }

}
