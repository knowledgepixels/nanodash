package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.component.ResourceTabs;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * A context page carrying a tab strip, standing in for the user, space and resource pages
 * in tests of what a tab switch carries along (issue #697). It mirrors {@link UserPage}:
 * the page's own resource is the context it hands on, while the {@code context} parameter
 * says where the reader came from.
 */
public class ResourceTabsNavTestPage extends NanodashPage {

    static final String USER_IRI = "https://orcid.org/0000-0002-1825-0097";

    public ResourceTabsNavTestPage(PageParameters parameters) {
        super(parameters);
        add(new TitleBar("titlebar", this));
        add(new Label("pagetitle", "Test User (user) | nanodash"));
        add(new ResourceTabs("tabs", "user", USER_IRI, ResourceTabs.Tab.CONTENT));
    }

    @Override
    public String getMountPath() {
        return "/resource-tabs-nav-test";
    }

    @Override
    public String getContextId() {
        return USER_IRI;
    }

    @Override
    public boolean isContextPage() {
        return true;
    }

}
