package com.knowledgepixels.nanodash.page;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.nanopub.extra.services.FailedApiCallException;

import com.knowledgepixels.nanodash.MaintainedResource;
import com.knowledgepixels.nanodash.component.TitleBar;

/**
 * This class represents a page for a maintained resource.
 */
public class ResourcePartPage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/part";

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

    public ResourcePartPage(final PageParameters parameters) throws FailedApiCallException {
        super(parameters);

        final String id = parameters.get("id").toString();
        final String contextId = parameters.get("context").toString();
        resource = MaintainedResource.get(contextId);

        add(new TitleBar("titlebar", this, "connectors"));

        // TODO Get this from nanopub:
        String label = id.replaceFirst("^.*[#/]([^#/]+)$", "$1");

        add(new Label("pagetitle", label + " (resource part) | nanodash"));
        add(new Label("name", label));
        add(new BookmarkablePageLink<Void>("id", ExplorePage.class, parameters.set("label", label)).setBody(Model.of(id)));
        //add(new BookmarkablePageLink<Void>("np", ExplorePage.class, new PageParameters().add("id", resource.getNanopubId())));

        add(new BookmarkablePageLink<Void>("resource", MaintainedResourcePage.class, new PageParameters().set("id", resource.getId())).setBody(Model.of(resource.getLabel())));


//        final List<AbstractLink> viewButtons = new ArrayList<>();
//        AbstractLink addViewButton = new BookmarkablePageLink<NanodashPage>("button", PublishPage.class, new PageParameters()
//                .add("template", "https://w3id.org/np/RA7vjbk3kz4FCu2eTX5oekZshPeOGNGTw8b2WLk8ZS7VI")
//                .add("param_resource", resource.getId())
//                .add("context", resource.getId())
//            );
//        addViewButton.setBody(Model.of("+"));
//        viewButtons.add(addViewButton);
//
//        if (resource.isDataInitialized()) {
//            add(new ViewList("views", resource));
//            add(new ButtonList("view-buttons", space, null, null, viewButtons));
//        } else {
//            add(new AjaxLazyLoadPanel<Component>("views") {
//    
//                @Override
//                public Component getLazyLoadComponent(String markupId) {
//                    return new ViewList(markupId, resource);
//                }
//    
//                @Override
//                protected boolean isContentReady() {
//                    return resource.isDataInitialized();
//                }
//    
//            });
//            add(new AjaxLazyLoadPanel<Component>("view-buttons") {
//    
//                @Override
//                public Component getLazyLoadComponent(String markupId) {
//                    return new ButtonList(markupId, space, null, null, viewButtons);
//                }
//    
//                @Override
//                protected boolean isContentReady() {
//                    return resource.isDataInitialized();
//                }
//
//                public Component getLoadingComponent(String id) {
//                    return new Label(id).setVisible(false);
//                };
//    
//            });
//        }
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
