package com.knowledgepixels.nanodash.page;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
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
import org.nanopub.extra.services.FailedApiCallException;
import org.nanopub.extra.services.QueryRef;

import com.knowledgepixels.nanodash.MaintainedResource;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.ButtonList;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.component.ViewList;

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

        QueryRef getDefQuery = new QueryRef("get-term-definitions", "term", id);
        for (IRI userIri : resource.getSpace().getUsers()) {
            for (String pubkey : User.getUserData().getPubkeyhashes(userIri, true)) {
                getDefQuery.getParams().put("pubkey", pubkey);
            }
        }
        ApiResponse getDefResp = QueryApiAccess.forcedGet(getDefQuery);
        if (getDefResp == null || getDefResp.getData().isEmpty()) {
            throw new RestartResponseException(ExplorePage.class, parameters);
        }
        String nanopubId = getDefResp.getData().iterator().next().get("np");
        Nanopub nanopub = Utils.getAsNanopub(nanopubId);

        String label = id.replaceFirst("^.*[#/]([^#/]+)$", "$1");
        String description = null;
        Set<IRI> classes = new HashSet<>();
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

        if (description != null) {
            add(new Label("description", description));
        } else {
            add(new Label("description").setVisible(false));
        }

        add(new Label("pagetitle", label + " (resource part) | nanodash"));
        add(new Label("name", label));
        add(new BookmarkablePageLink<Void>("id", ExplorePage.class, parameters.set("label", label)).setBody(Model.of(id)));
        add(new BookmarkablePageLink<Void>("np", ExplorePage.class, new PageParameters().add("id", nanopubId)));

        add(new BookmarkablePageLink<Void>("resource", MaintainedResourcePage.class, new PageParameters().set("id", resource.getId())).setBody(Model.of(resource.getLabel())));


        final List<AbstractLink> viewButtons = new ArrayList<>();
        AbstractLink addViewButton = new BookmarkablePageLink<NanodashPage>("button", PublishPage.class, new PageParameters()
                .add("template", "https://w3id.org/np/RA7vjbk3kz4FCu2eTX5oekZshPeOGNGTw8b2WLk8ZS7VI")
                .add("param_resource", resource.getId())
                .add("context", resource.getId())
            );
        addViewButton.setBody(Model.of("+"));
        viewButtons.add(addViewButton);

        if (resource.isDataInitialized()) {
            add(new ViewList("views", resource, id, classes));
            add(new ButtonList("view-buttons", resource.getSpace(), null, null, viewButtons));
        } else {
            add(new AjaxLazyLoadPanel<Component>("views") {
    
                @Override
                public Component getLazyLoadComponent(String markupId) {
                    return new ViewList(markupId, resource, id, classes);
                }
    
                @Override
                protected boolean isContentReady() {
                    return resource.isDataInitialized();
                }
    
            });
            add(new AjaxLazyLoadPanel<Component>("view-buttons") {
    
                @Override
                public Component getLazyLoadComponent(String markupId) {
                    return new ButtonList(markupId, resource.getSpace(), null, null, viewButtons);
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
