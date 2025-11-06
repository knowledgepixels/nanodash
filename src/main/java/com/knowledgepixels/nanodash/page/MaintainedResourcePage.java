package com.knowledgepixels.nanodash.page;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.FailedApiCallException;

import com.knowledgepixels.nanodash.MaintainedResource;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.component.ButtonList;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.component.ViewList;

/**
 * This class represents a page for a maintained resource.
 */
public class MaintainedResourcePage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/resource";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Maintained resource object with the data shown on this page.
     */
    private MaintainedResource resource;

    public MaintainedResourcePage(final PageParameters parameters) throws FailedApiCallException {
        super(parameters);

        resource = MaintainedResource.get(parameters.get("id").toString());

        add(new TitleBar("titlebar", this, "connectors"));

        add(new Label("pagetitle", resource.getLabel() + " (resource) | nanodash"));
        add(new Label("resourcename", resource.getLabel()));
        add(new BookmarkablePageLink<Void>("id", ExplorePage.class, parameters.set("label", resource.getLabel())).setBody(Model.of(resource.getId())));
        add(new BookmarkablePageLink<Void>("np", ExplorePage.class, new PageParameters().set("id", resource.getNanopubId())));

        String namespaceUri = resource.getNamespace() == null ? "" : resource.getNamespace();
        add(new BookmarkablePageLink<Void>("namespace", ExplorePage.class, new PageParameters().set("id", namespaceUri)).setBody(Model.of(namespaceUri)));

        Space space = resource.getSpace();
        add(new BookmarkablePageLink<Void>("space", SpacePage.class, new PageParameters().set("id", space.getId())).setBody(Model.of(space.getLabel())));

        final List<AbstractLink> viewButtons = new ArrayList<>();
        AbstractLink addViewButton = new BookmarkablePageLink<NanodashPage>("button", PublishPage.class, new PageParameters()
                .set("template", "https://w3id.org/np/RAxERE0cQ9jLQZ5VjeA-1v3XnE9ugxLpFG8vpkAd5FqHE")
                .set("param_resource", resource.getId())
                .set("context", resource.getId())
            );
        addViewButton.setBody(Model.of("+ view"));
        viewButtons.add(addViewButton);

        if (resource.isDataInitialized()) {
            add(new ViewList("views", resource));
            add(new ButtonList("view-buttons", space, null, null, viewButtons));
        } else {
            add(new AjaxLazyLoadPanel<Component>("views") {
    
                @Override
                public Component getLazyLoadComponent(String markupId) {
                    return new ViewList(markupId, resource);
                }
    
                @Override
                protected boolean isContentReady() {
                    return resource.isDataInitialized();
                }
    
            });
            add(new AjaxLazyLoadPanel<Component>("view-buttons") {
    
                @Override
                public Component getLazyLoadComponent(String markupId) {
                    return new ButtonList(markupId, space, null, null, viewButtons);
                }
    
                @Override
                protected boolean isContentReady() {
                    return resource.isDataInitialized();
                }

                public Component getLoadingComponent(String id) {
                    return new Label(id).setVisible(false);
                };
    
            });
        }
    }

    /**
     * Checks if auto-refresh is enabled for this page.
     *
     * @return true if auto-refresh is enabled, false otherwise
     */
    protected boolean hasAutoRefreshEnabled() {
        return true;
    }

}
