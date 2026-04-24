package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.Utils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import org.nanopub.vocabulary.NTEMPLATE;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateTest {

    // Template: Defining a new class
    private final String templateUri = "https://w3id.org/np/RAJetZMP40rNpwVYsUpYA5_psx-paQ6pf5Gu9iz9Vmwak";

    // Template: "Testing root nanopub placeholder" — has a sub:rootNanopub typed as nt:RootNanopubPlaceholder.
    private final String rootNpPlaceholderTemplateUri = "https://w3id.org/np/RAcws59tX-7nxdPpgl6FhRNq5KLIwJBJSZsHilqmOyT8Q";

    @Test
    void defaultConstructor() throws MalformedTemplateException {
        Template template = new Template(templateUri);
        assertNotNull(template);
    }

    @Test
    void getLabel() throws MalformedTemplateException {
        Template template = new Template(templateUri);
        String templateLabel = "Defining a new class";
        assertEquals(templateLabel, template.getLabel());
    }

    @Test
    void getDescription() throws MalformedTemplateException {
        Template template = new Template(templateUri);
        String templateDescription = """
                <p>Such a nanopublication defines a new class. Classes represent sets of concrete or abstract things, and are by convention named with singular nouns (or noun phrases) like 'human', 'cardiovascular disease', or 'approach'.</p>
                
                <p>If the term you want to define does not refer to a set of things but a single instance, such as the planet Mars or Marie Curie, define them with the template for individuals instead.</p>""";
        assertEquals(Utils.sanitizeHtml(templateDescription), template.getDescription().replace("\r\n", "\n"));
    }

    @Test
    void getTag() throws MalformedTemplateException {
        Template template = new Template(templateUri);
        String templateTag = "Terms";
        assertEquals(templateTag, template.getTag());
    }

    @Test
    void getNanopubLabelPattern() throws MalformedTemplateException {
        Template template = new Template(templateUri);
        String nanopubLabelPattern = "Class: ${name}";
        assertEquals(nanopubLabelPattern, template.getNanopubLabelPattern());
    }

    @Test
    void invokeLookupApiForWikidata() throws Exception {
        Template template = new Template(templateUri);
        IRI relatedIdentity = SimpleValueFactory.getInstance().createIRI("https://w3id.org/np/RAJetZMP40rNpwVYsUpYA5_psx-paQ6pf5Gu9iz9Vmwak/relatedentity");
        Map<String, String> resultMap = new HashMap<>();
        template.getPossibleValuesFromApi(relatedIdentity, "dog", resultMap);
        assertFalse(resultMap.isEmpty());
    }

    @Test
    void isRootNanopubPlaceholderRecognizesTypedSlot() throws MalformedTemplateException {
        Template template = new Template(rootNpPlaceholderTemplateUri);
        ValueFactory vf = SimpleValueFactory.getInstance();
        IRI rootSlot = vf.createIRI(rootNpPlaceholderTemplateUri + "/rootNanopub");
        assertTrue(template.isRootNanopubPlaceholder(rootSlot));
        assertTrue(template.isPlaceholder(rootSlot));
    }

    @Test
    void isRootNanopubPlaceholderFalseForOtherSlots() throws MalformedTemplateException {
        Template template = new Template(rootNpPlaceholderTemplateUri);
        ValueFactory vf = SimpleValueFactory.getInstance();
        IRI literalSlot = vf.createIRI(rootNpPlaceholderTemplateUri + "/message");
        assertFalse(template.isRootNanopubPlaceholder(literalSlot));
        // Sanity check: the class IRI itself isn't a (typed) placeholder in this template.
        assertFalse(template.isRootNanopubPlaceholder(NTEMPLATE.ROOT_NANOPUB_PLACEHOLDER));
    }

}