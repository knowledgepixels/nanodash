package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.Utils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TemplateTest {

    // Template: Defining a new class
    private final String templateUri = "https://w3id.org/np/RAJetZMP40rNpwVYsUpYA5_psx-paQ6pf5Gu9iz9Vmwak";

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

}