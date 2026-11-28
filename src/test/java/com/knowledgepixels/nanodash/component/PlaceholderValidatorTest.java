package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.DynamicPrefix;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.WicketApplication;
import com.knowledgepixels.nanodash.template.ContextType;
import com.knowledgepixels.nanodash.template.TemplateContext;
import com.knowledgepixels.nanodash.template.TemplateData;
import org.apache.wicket.util.tester.WicketTester;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nanopub.NanopubCreator;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A field checks what is entered into it by the rules nanopub-java makes the nanopublication by,
 * and tells each problem in the words the form has used for it (#936).
 */
class PlaceholderValidatorTest {

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static final String TEMPLATE_URI = "https://w3id.org/np/RAAbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_Val01";
    private static final IRI SUBJECT = vf.createIRI("http://example.com/subject");
    private static final IRI PAGE = iri("page");
    private static final IRI PAPER = iri("paper");
    private static final IRI EXTERNAL = iri("external");
    private static final IRI TRUSTY = iri("trusty");
    private static final IRI VALUE = iri("value");
    private static final IRI RESOURCE = iri("resource");
    private static final IRI NAME = iri("name");
    private static final IRI COUNT = iri("count");

    private WicketTester tester;
    private TemplateContext context;

    private static IRI iri(String name) {
        return vf.createIRI(TEMPLATE_URI + "/" + name);
    }

    @BeforeEach
    void setUp() throws Exception {
        tester = new WicketTester(new WicketApplication());
        NanopubCreator creator = new NanopubCreator(TEMPLATE_URI);
        IRI node = creator.getAssertionUri();
        creator.addAssertionStatement(node, RDF.TYPE, NTEMPLATE.ASSERTION_TEMPLATE);
        creator.addAssertionStatement(node, RDFS.LABEL, vf.createLiteral("Field check test template"));
        int n = 0;
        for (IRI placeholder : List.of(PAGE, PAPER, EXTERNAL, TRUSTY, VALUE, RESOURCE, NAME, COUNT)) {
            IRI st = iri("st" + (++n));
            creator.addAssertionStatement(node, NTEMPLATE.HAS_STATEMENT, st);
            creator.addAssertionStatement(st, RDF.SUBJECT, SUBJECT);
            creator.addAssertionStatement(st, RDF.PREDICATE, vf.createIRI("http://example.com/p" + n));
            creator.addAssertionStatement(st, RDF.OBJECT, placeholder);
            creator.addAssertionStatement(placeholder, RDFS.LABEL, vf.createLiteral(Utils.getUriPostfix(placeholder)));
        }
        creator.addAssertionStatement(PAGE, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(PAPER, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(PAPER, NTEMPLATE.HAS_PREFIX, vf.createLiteral("https://doi.org/"));
        creator.addAssertionStatement(PAPER, NTEMPLATE.HAS_REGEX, vf.createLiteral("10\\.[0-9]+/\\S+"));
        creator.addAssertionStatement(EXTERNAL, RDF.TYPE, NTEMPLATE.EXTERNAL_URI_PLACEHOLDER);
        creator.addAssertionStatement(TRUSTY, RDF.TYPE, NTEMPLATE.TRUSTY_URI_PLACEHOLDER);
        creator.addAssertionStatement(VALUE, RDF.TYPE, NTEMPLATE.VALUE_PLACEHOLDER);
        creator.addAssertionStatement(RESOURCE, RDF.TYPE, NTEMPLATE.URI_PLACEHOLDER);
        creator.addAssertionStatement(RESOURCE, NTEMPLATE.HAS_PREFIX, vf.createLiteral(DynamicPrefix.SPACE_TOKEN + "/r/"));
        creator.addAssertionStatement(NAME, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(NAME, NTEMPLATE.HAS_REGEX, vf.createLiteral("[A-Z].*"));
        creator.addAssertionStatement(COUNT, RDF.TYPE, NTEMPLATE.LITERAL_PLACEHOLDER);
        creator.addAssertionStatement(COUNT, NTEMPLATE.HAS_DATATYPE, XSD.INT);
        creator.addProvenanceStatement(vf.createStatement(creator.getAssertionUri(), RDFS.SEEALSO, creator.getAssertionUri()));
        creator.addPubinfoStatement(vf.createStatement(creator.getNanopubUri(), RDFS.SEEALSO, creator.getNanopubUri()));
        TemplateData.get().registerTemplate(creator.finalizeNanopub());
        context = new TemplateContext(ContextType.ASSERTION, TEMPLATE_URI, "statement", (String) null);
    }

    @AfterEach
    void tearDown() {
        tester.destroy();
    }

    private List<String> problems(IRI placeholder, String text) {
        return new PlaceholderValidator(context, placeholder).getProblems(text);
    }

    @Test
    void aValidValueHasNoProblems() {
        assertEquals(List.of(), problems(PAGE, "https://example.com/x"));
        assertEquals(List.of(), problems(PAGE, "my-thing"), "a plain name is minted within the nanopublication");
        assertEquals(List.of(), problems(PAPER, "10.1234/abc"));
        assertEquals(List.of(), problems(PAPER, "https://doi.org/10.1234/abc"));
        assertEquals(List.of(), problems(EXTERNAL, "https://example.com/x"));
        assertEquals(List.of(), problems(VALUE, "\"some text\""));
        assertEquals(List.of(), problems(VALUE, "https://example.com/x"));
        assertEquals(List.of(), problems(NAME, "Alice"));
        assertEquals(List.of(), problems(COUNT, "42"));
        assertEquals(List.of(), problems(PAGE, ""), "an empty field is the required check's to tell");
    }

    @Test
    void problemsAreToldInTheWordsOfTheForm() {
        assertEquals(List.of("IRI not well-formed"), problems(PAGE, "two words"));
        assertEquals(List.of("IRI scheme not allowed here; use one of: " + Utils.getAllowedUriSchemesLabel()),
                problems(VALUE, "urn:isbn:0451450523"));
        assertEquals(List.of("Invalid literal value"), problems(VALUE, "\"unterminated"));
        assertEquals(List.of("Not an external IRI"), problems(EXTERNAL, "something"));
        assertEquals(List.of("Not a trusty URI"), problems(TRUSTY, "https://example.com/x"));
    }

    @Test
    void aPartNameOfTheNanopublicationIsRefused() {
        assertEquals(List.of("'assertion' is what this nanopublication calls one of its own parts, so a resource of that"
                + " name would be that part rather than something the nanopublication is about. Pick a different name."),
                problems(PAGE, "assertion"));
        assertEquals(1, problems(PAGE, "pubinfo").size());
        assertEquals(List.of("Not an external IRI"), problems(EXTERNAL, "assertion"), "nothing is minted for an external IRI");
    }

    @Test
    void aPatternIsCheckedOnWhatFollowsThePrefix() {
        assertEquals(List.of("Value '11.1234/abc' of 'paper' doesn't match the pattern '10\\.[0-9]+/\\S+'"),
                problems(PAPER, "11.1234/abc"));
        assertEquals(List.of("Value '11.1234/abc' of 'paper' doesn't match the pattern '10\\.[0-9]+/\\S+'"),
                problems(PAPER, "https://doi.org/11.1234/abc"), "the IRI in full is taken by what follows the prefix");
    }

    @Test
    void textThatIsNeitherAnIriNorANameIsToldInTheWordsOfTheLibrary() {
        List<String> problems = problems(PAPER, "10.1234/a:b");
        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("'10.1234/a:b' is neither an IRI in one of the schemes"), problems.toString());
    }

    @Test
    void anUnresolvedSpacePrefixIsLeftToThePicker() {
        assertTrue(context.hasUnresolvedPrefix(RESOURCE));
        assertEquals(List.of(), problems(RESOURCE, "thing"));
    }

    @Test
    void aLiteralIsCheckedForItsPatternAndDatatype() {
        assertEquals(List.of("Value 'alice' of 'name' doesn't match the pattern '[A-Z].*'"), problems(NAME, "alice"));
        assertEquals(List.of("Value 'abc' of 'count' is not a valid xsd:int"), problems(COUNT, "abc"));
    }

}
