package com.knowledgepixels.nanodash.template;

import org.nanopub.templates.Template;
import org.nanopub.templates.MalformedTemplateException;

import org.nanopub.Nanopub;

/**
 * Parses templates for tests.
 */
public class TemplateTestUtil {

    private TemplateTestUtil() {
    }

    /**
     * Parses a template from the given nanopub.
     *
     * @param np the template nanopub
     * @return the parsed template
     * @throws MalformedTemplateException if the template is malformed
     */
    public static Template parseTemplate(Nanopub np) throws MalformedTemplateException {
        return new Template(np);
    }

}
