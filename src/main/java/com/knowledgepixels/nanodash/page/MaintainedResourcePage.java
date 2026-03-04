package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.component.*;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.util.Values;

import java.util.ArrayList;
import java.util.List;

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
    private final MaintainedResource resource;

    public MaintainedResourcePage(final PageParameters parameters) {
        super(parameters);

        resource = MaintainedResourceRepository.get().findById(parameters.get("id").toString());
        Space space = resource.getSpace();
        resource.triggerDataUpdate();

        List<AbstractResourceWithProfile> superSpaces = resource.getAllSuperSpacesUntilRoot();
        superSpaces.add(resource.getSpace());
        superSpaces.add(resource);
        add(new TitleBar("titlebar", this, null,
                superSpaces.stream().map(ss -> new NanodashPageRef(SpacePage.class, new PageParameters().add("id", ss.getId()), ss.getLabel())).toArray(NanodashPageRef[]::new)
        ));

        add(new JustPublishedMessagePanel("justPublishedMessage", parameters));

        add(new Label("pagetitle", resource.getLabel() + " (resource) | nanodash"));
        add(new Label("resourcename", resource.getLabel()));
        add(new ExternalLinkWithActionsPanel("id", Model.of(resource.getId()), Model.of(resource.getLabel())));
        add(new SourceNanopub("np", Values.iri(resource.getNanopubId())));

        String namespaceUri = resource.getNamespace() == null ? "" : resource.getNamespace();
        add(new BookmarkablePageLink<Void>("namespace", ExplorePage.class, new PageParameters().set("id", namespaceUri)).setBody(Model.of(namespaceUri)));

        final List<AbstractLink> viewButtons = new ArrayList<>();
        viewButtons.add(new AddViewDisplayButton("button",
                        "https://w3id.org/np/RAe0zantvnJlVWIC2LueG1IAMktXGFIqCdWliok1rOrmU",
                        "latest",
                        resource.getId(),
                        resource.getId(),
                        new PageParameters()
                                .set("param_appliesToResource", resource.getId())
                                .set("refresh-upon-publish", resource.getId())
                )
        );

        if (resource.isDataInitialized()) {
            add(new ViewList("views", resource, null, null, null, space, viewButtons));
        } else {
            add(new AjaxLazyLoadPanel<Component>("views") {

                @Override
                public Component getLazyLoadComponent(String markupId) {
                    return new ViewList(markupId, resource, null, null, null, space, viewButtons);
                }

                @Override
                protected boolean isContentReady() {
                    return resource.isDataInitialized();
                }

                @Override
                public Component getLoadingComponent(String id) {
                    return new Label(id, "<div class=\"row-section\"><div class=\"col-12\">" + ResultComponent.getWaitIconHtml() + "</div></div>").setEscapeModelStrings(false);
                }

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
