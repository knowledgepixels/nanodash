package com.knowledgepixels.nanodash.template;

import com.knowledgepixels.nanodash.DynamicPrefix;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
import org.nanopub.Nanopub;
import org.nanopub.NanopubImpl;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Throwaway check that the hand-written test template parses and carries the expected
 * dynamic prefixes. Delete once the template is published.
 */
public class ScratchTemplateCheck {

    private static final String PATH = System.getProperty("scratch.trig", "");

    @Test
    void parsesAndCarriesDynamicPrefixes() throws Exception {
        if (PATH.isEmpty()) return;
        Nanopub np = new NanopubImpl(new File(PATH));
        Template t = TemplateTestUtil.parseTemplate(np);
        String base = np.getUri().stringValue();
        System.out.println("LABEL: " + t.getLabel());
        System.out.println("STATEMENTS: " + t.getStatementIris());
        System.out.println("ERRORS: " + t.getStatementErrors());
        assertTrue(t.getStatementErrors().isEmpty(), "template must have no statement errors");

        IRI item = SimpleValueFactory.getInstance().createIRI(base + "#item");
        IRI parent = SimpleValueFactory.getInstance().createIRI(base + "#parent");
        System.out.println("item prefix:   " + t.getPrefix(item));
        System.out.println("parent prefix: " + t.getPrefix(parent));
        assertEquals("~~SPACE~~/r/", t.getPrefix(item));
        assertEquals("~~NAMESPACE~~", t.getPrefix(parent));
        assertEquals(DynamicPrefix.SPACE_TOKEN, DynamicPrefix.getToken(t.getPrefix(item)));
        assertEquals(DynamicPrefix.NAMESPACE_TOKEN, DynamicPrefix.getToken(t.getPrefix(parent)));
        assertTrue(t.isUriPlaceholder(item));
        assertNotNull(t.getLabel(item));
    }

}
