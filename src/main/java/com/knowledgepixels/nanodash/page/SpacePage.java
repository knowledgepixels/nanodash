package com.knowledgepixels.nanodash.page;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.FailedApiCallException;

import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.ItemListElement;
import com.knowledgepixels.nanodash.component.ItemListPanel;
import com.knowledgepixels.nanodash.component.PinGroupList;
import com.knowledgepixels.nanodash.component.TitleBar;



/**
 * The ProjectPage class represents a space page in the Nanodash application.
 */
public class SpacePage extends NanodashPage {

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/space";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Space object with the data shown on this page.
     */
    private Space space;

    /**
     * Constructor for the SpacePage.
     *
     * @param parameters the page parameters
     * @throws org.nanopub.extra.services.FailedApiCallException if the API call fails
     */
    public SpacePage(final PageParameters parameters) throws FailedApiCallException {
        super(parameters);

        space = Space.get(parameters.get("id").toString());
        Nanopub np = space.getRootNanopub();

        add(new TitleBar("titlebar", this, "connectors"));

        add(new Label("pagetitle", space.getLabel() + " (space) | nanodash"));
        add(new Label("spacename", space.getLabel()));
        add(new Label("spacetype", space.getTypeLabel()));
        add(new ExternalLink("id", space.getId(), space.getId()));
        add(new BookmarkablePageLink<Void>("np", ExplorePage.class, new PageParameters().add("id", np.getUri())));
        add(new Label("description", "<span>" + Utils.sanitizeHtml(space.getDescription()) + "</span>").setEscapeModelStrings(false));

        if (space.isDataInitialized()) {
            add(new PinGroupList("pin-groups", space));
        } else {
            add(new AjaxLazyLoadPanel<Component>("pin-groups") {
    
                @Override
                public Component getLazyLoadComponent(String markupId) {
                    return new PinGroupList(markupId, space);
                }
    
                @Override
                protected boolean isContentReady() {
                    return space.isDataInitialized();
                }
    
            });
        }

        add(new ItemListPanel<Pair<IRI, String>>(
                "members",
                "Members",
                () -> space.isDataInitialized(),
                () -> {
                        List<Pair<IRI, String>> members = new ArrayList<>();
                        Set<IRI> ownerSet = new HashSet<>(space.getOwners());
                        for (IRI owner : space.getOwners()) members.add(Pair.of(owner, "(owner)"));
                        for (IRI member : space.getMembers()) {
                            if (ownerSet.contains(member)) continue;
                            members.add(Pair.of(member, ""));
                        }
                        return members;
                    },
                (p) -> new ItemListElement("item", UserPage.class, new PageParameters().add("id", p.getLeft()), User.getShortDisplayName(p.getLeft()), p.getRight())
            ));

        add(new ItemListPanel<Space>(
                "superspaces",
                "Part of",
                space.getSuperspaces(),
                (space) -> new ItemListElement("item", SpacePage.class, new PageParameters().add("id", space), space.getLabel(), "(" + space.getTypeLabel() + ")")
            ));

        addSubspacePanel("Group");
        addSubspacePanel("Project");
        addSubspacePanel("Program");
        addSubspacePanel("Initiative");
        addSubspacePanel("Community");
        addSubspacePanel("Event");

    }

    private void addSubspacePanel(String type) {
        String typePl = type + "s";
        typePl = typePl.replaceFirst("ys$", "ies");

        add(new ItemListPanel<Space>(
                typePl.toLowerCase(),
                typePl,
                space.getSubspaces("https://w3id.org/kpxl/gen/terms/" + type),
                (space) -> new ItemListElement("item", SpacePage.class, new PageParameters().add("id", space), space.getLabel())
            ));
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
