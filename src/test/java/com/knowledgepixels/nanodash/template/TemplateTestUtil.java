package com.knowledgepixels.nanodash.template;

import org.nanopub.Nanopub;

/**
 * Gives tests outside this package access to the package-private Template constructor.
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
