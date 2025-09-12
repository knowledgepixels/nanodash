package com.knowledgepixels.nanodash.page;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.Nanopub;
import org.nanopub.extra.services.FailedApiCallException;

import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.ItemListElement;
import com.knowledgepixels.nanodash.component.ItemListPanel;
import com.knowledgepixels.nanodash.component.QueryResultTable;
import com.knowledgepixels.nanodash.component.TemplateItem;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.template.Template;

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
        add(new ExternalLink("id", space.getId(), space.getId()));
        add(new BookmarkablePageLink<Void>("np", ExplorePage.class, new PageParameters().add("id", np.getUri())));
        add(new Label("description", "<span>" + Utils.sanitizeHtml(space.getDescription()) + "</span>").setEscapeModelStrings(false));

        final PageParameters params = new PageParameters();
        if (space.getDefaultProvenance() != null) {
            params.add("prtemplate", space.getDefaultProvenance().stringValue());
        }
        List<Pair<String, List<Template>>> templateLists = new ArrayList<>();
        List<String> templateTagList = new ArrayList<>(space.getTemplateTags());
        Collections.sort(templateTagList);
        List<Template> templates = new ArrayList<>(space.getTemplates());
        for (String tag : templateTagList) {
            for (Template t : space.getTemplatesPerTag().get(tag)) {
                if (templates.contains(t)) templates.remove(t);
            }
            templateLists.add(Pair.of(tag, space.getTemplatesPerTag().get(tag)));
        }
        if (!templates.isEmpty()) {
            String l = templateLists.isEmpty() ? "Templates" : "Other Templates";
            templateLists.add(Pair.of(l, templates));
        }
        add(new DataView<Pair<String, List<Template>>>("template-lists", new ListDataProvider<>(templateLists)) {

            @Override
            protected void populateItem(Item<Pair<String, List<Template>>> item) {
                item.add(new ItemListPanel<Template>(
                        "templates",
                        item.getModelObject().getLeft(),
                        item.getModelObject().getRight(),
                        (template) -> new TemplateItem("item", template, params)
                    ));
            }

        });

        add(new ItemListPanel<Template>(
                "templates",
                "Templates",
                () -> space.isDataInitialized(),
                () -> space.getTemplates(),
                (template) -> new TemplateItem("item", template, params)
            ));

        add(new ItemListPanel<IRI>(
                "owner-users",
                "Owners",
                () -> space.isDataInitialized(),
                () -> space.getOwners(),
                (userIri) -> new ItemListElement("item", UserPage.class, new PageParameters().add("id", userIri), User.getShortDisplayName(userIri))
            ));

        add(new ItemListPanel<IRI>(
                "member-users",
                "Members",
                () -> space.isDataInitialized(),
                () -> space.getMembers(),
                (userIri) -> new ItemListElement("item", UserPage.class, new PageParameters().add("id", userIri), User.getShortDisplayName(userIri))
            ));

        add(new DataView<IRI>("queries", new ListDataProvider<IRI>(space.getQueryIds())) {

            @Override
            protected void populateItem(Item<IRI> item) {
                String queryId = QueryApiAccess.getQueryId(item.getModelObject());
                item.add(QueryResultTable.createComponent("query", queryId, false));
            }

        });
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
