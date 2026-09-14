package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.Nanopub;

import java.util.List;

/**
 * A page about a fixed resource, standing in for the space, user, resource and part pages
 * in tests of what {@link NanodashPage} does with an {@link RdfSource} (issue #710).
 */
public class RdfSourceTestPage extends NanodashPage {

    /**
     * The nanopublication the page's resource is declared by; set by the test before the
     * page is started, since a page under the tester is constructed by Wicket.
     */
    static Nanopub declaration;

    static final String RESOURCE_IRI = "https://example.org/spaces/test-workshop";

    public RdfSourceTestPage(PageParameters parameters) {
        super(parameters);
        redirectIfRdfRequested(getRdfSource());
        add(new TitleBar("titlebar", this));
        add(new Label("pagetitle", "Test Workshop (space) | nanodash"));
    }

    @Override
    public String getMountPath() {
        return "/rdf-source-test";
    }

    @Override
    protected RdfSource getRdfSource() {
        return new RdfSource("space", RESOURCE_IRI, null, declaration == null ? List.of() : List.of(declaration));
    }

}
