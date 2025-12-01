package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.component.ButtonList;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.component.ViewList;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * Profiled resource (Space or MaintainedResource) object with the data shown on this page.
     */
    private ProfiledResource profiledResource;

    public ResourcePartPage(final PageParameters parameters) {
        super(parameters);

        final String id = parameters.get("id").toString();
        final String contextId = parameters.get("context").toString();
        profiledResource = MaintainedResource.get(contextId);
        if (profiledResource == null) {
            if (Space.get(contextId) == null) {
                throw new IllegalArgumentException("Not a resource or space: " + contextId);
            }
            profiledResource = Space.get(contextId);
        }

        List<ProfiledResource> superSpaces = profiledResource.getAllSuperSpacesUntilRoot();
        superSpaces.add(profiledResource);
        add(new TitleBar("titlebar", this, null,
                superSpaces.stream().map(ss -> new NanodashPageRef(SpacePage.class, new PageParameters().add("id", ss.getId()), ss.getLabel())).toArray(NanodashPageRef[]::new)
        ));

        QueryRef getDefQuery = new QueryRef("get-term-definitions", "term", id);
        for (IRI userIri : profiledResource.getSpace().getUsers()) {
            for (String pubkey : User.getUserData().getPubkeyhashes(userIri, true)) {
                getDefQuery.getParams().put("pubkey", pubkey);
            }
        }

        final String nanopubId;
        String label = id.replaceFirst("^.*[#/]([^#/]+)$", "$1");
        String description = null;
        Set<IRI> classes = new HashSet<>();

        ApiResponse getDefResp = ApiCache.retrieveResponse(getDefQuery);
        if (getDefResp == null) {
            getDefResp = QueryApiAccess.forcedGet(getDefQuery);
        }
        if (getDefResp != null && !getDefResp.getData().isEmpty()) {
            nanopubId = getDefResp.getData().iterator().next().get("np");

            Nanopub nanopub = Utils.getAsNanopub(nanopubId);
            for (Statement st : nanopub.getAssertion()) {
                if (!st.getSubject().stringValue().equals(id)) continue;
                if (st.getPredicate().equals(RDFS.LABEL)) label = st.getObject().stringValue();
                if (st.getPredicate().equals(SKOS.DEFINITION) || st.getPredicate().equals(DCTERMS.DESCRIPTION) || st.getPredicate().equals(RDFS.COMMENT)) {
                    description = st.getObject().stringValue();
                }
                if (st.getPredicate().equals(RDF.TYPE) && st.getObject() instanceof IRI objIri) {
                    classes.add(objIri);
                }
            }
        } else {
            nanopubId = null;
        }
//        if (getDefResp == null || getDefResp.getData().isEmpty()) {
//            throw new RestartResponseException(ExplorePage.class, parameters);
//        }


        if (description != null) {
            add(new Label("description", description));
        } else {
            add(new Label("description").setVisible(false));
        }

        add(new Label("pagetitle", label + " (resource part) | nanodash"));
        add(new Label("name", label));
        add(new BookmarkablePageLink<Void>("id", ExplorePage.class, parameters.set("label", label)).setBody(Model.of(id)));
        add(new BookmarkablePageLink<Void>("np", ExplorePage.class, new PageParameters().set("id", nanopubId == null ? id : nanopubId)));

        // TODO Improve this code, e.g. make Space a subclass of MaintainedResource or otherwise refactor:
        // we now use the ProfileResource abstraction, but the code still has to be imprved
        if (profiledResource != null) {
            add(new BookmarkablePageLink<Void>("resource", MaintainedResourcePage.class, new PageParameters().set("id", profiledResource.getId())).setBody(Model.of(profiledResource.getLabel())));

            final List<AbstractLink> viewButtons = new ArrayList<>();
            AbstractLink addViewButton = new BookmarkablePageLink<NanodashPage>("button", PublishPage.class, new PageParameters()
                    .set("template", "https://w3id.org/np/RAxERE0cQ9jLQZ5VjeA-1v3XnE9ugxLpFG8vpkAd5FqHE")
                    .set("param_displayType", KPXL_TERMS.PART_LEVEL_VIEW_DISPLAY)
                    .set("param_resource", profiledResource.getId())
                    .set("context", profiledResource.getId())
            );
            addViewButton.setBody(Model.of("+ view"));
            viewButtons.add(addViewButton);

            final String nanopubRef = nanopubId == null ? "x:" : nanopubId;
            if (profiledResource.isDataInitialized()) {
                add(new ViewList("views", profiledResource, id, nanopubRef, classes));
                add(new ButtonList("view-buttons", profiledResource.getSpace(), null, null, viewButtons));
            } else {
                add(new AjaxLazyLoadPanel<Component>("views") {

                    @Override
                    public Component getLazyLoadComponent(String markupId) {
                        return new ViewList(markupId, profiledResource, id, nanopubRef, classes);
                    }

                    @Override
                    protected boolean isContentReady() {
                        return profiledResource.isDataInitialized();
                    }

                });
                add(new AjaxLazyLoadPanel<Component>("view-buttons") {

                    @Override
                    public Component getLazyLoadComponent(String markupId) {
                        return new ButtonList(markupId, profiledResource.getSpace(), null, null, viewButtons);
                    }

                    @Override
                    protected boolean isContentReady() {
                        return profiledResource.isDataInitialized();
                    }

                    public Component getLoadingComponent(String id) {
                        return new Label(id).setVisible(false);
                    }

                });
            }
        } else {
            // TODO Ugly code duplication (see above):

            add(new BookmarkablePageLink<Void>("resource", SpacePage.class, new PageParameters().set("id", profiledResource.getSpace().getId())).setBody(Model.of(profiledResource.getSpace().getLabel())));

            final List<AbstractLink> viewButtons = new ArrayList<>();
            AbstractLink addViewButton = new BookmarkablePageLink<NanodashPage>("button", PublishPage.class, new PageParameters()
                    .set("template", "https://w3id.org/np/RAxERE0cQ9jLQZ5VjeA-1v3XnE9ugxLpFG8vpkAd5FqHE")
                    .set("param_displayType", KPXL_TERMS.PART_LEVEL_VIEW_DISPLAY)
                    .set("param_resource", profiledResource.getSpace().getId())
                    .set("context", profiledResource.getSpace().getId())
            );
            addViewButton.setBody(Model.of("+ view"));
            viewButtons.add(addViewButton);

            if (profiledResource.getSpace().isDataInitialized()) {
                add(new ViewList("views", profiledResource.getSpace(), id, nanopubId, classes));
                add(new ButtonList("view-buttons", profiledResource.getSpace(), null, null, viewButtons));
            } else {
                add(new AjaxLazyLoadPanel<Component>("views") {

                    @Override
                    public Component getLazyLoadComponent(String markupId) {
                        return new ViewList(markupId, profiledResource.getSpace(), id, nanopubId, classes);
                    }

                    @Override
                    protected boolean isContentReady() {
                        return profiledResource.getSpace().isDataInitialized();
                    }

                });
                add(new AjaxLazyLoadPanel<Component>("view-buttons") {

                    @Override
                    public Component getLazyLoadComponent(String markupId) {
                        return new ButtonList(markupId, profiledResource.getSpace(), null, null, viewButtons);
                    }

                    @Override
                    protected boolean isContentReady() {
                        return profiledResource.getSpace().isDataInitialized();
                    }

                    public Component getLoadingComponent(String id) {
                        return new Label(id).setVisible(false);
                    }

                });
            }
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
