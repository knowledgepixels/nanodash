package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.page.UserPage;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.ContextRelativeResourceReference;
import org.apache.wicket.util.string.Strings;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

/**
 * Component for displaying query results as a vertical item list.
 * Each result row is rendered as a single linked item, with icon handling
 * for user_iri and template_iri columns.
 */
public class QueryResultItemList extends QueryResult {

    private FilteredQueryResultDataProvider filteredDataProvider;
    private final Model<String> filterModel = Model.of("");
    private WebMarkupContainer itemsContainer;

    QueryResultItemList(String markupId, QueryRef queryRef, ApiResponse response, ViewDisplay viewDisplay) {
        super(markupId, queryRef, response, viewDisplay);

        String label = grlcQuery.getLabel();
        if (viewDisplay.getTitle() != null) {
            label = viewDisplay.getTitle();
        }
        add(new Label("label", label));
        setOutputMarkupId(true);

        TextField<String> filterField = new TextField<>("filter", filterModel);
        filterField.setOutputMarkupId(true);
        filterField.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (filteredDataProvider != null && itemsContainer != null) {
                    filteredDataProvider.setFilterText(filterModel.getObject());
                    target.add(itemsContainer);
                }
            }
        });
        add(filterField);

        populateComponent();
    }

    @Override
    protected void populateComponent() {
        QueryResultDataProvider dataProvider = new QueryResultDataProvider(response.getData());
        filteredDataProvider = new FilteredQueryResultDataProvider(dataProvider, response);

        DataView<ApiResponseEntry> dataView = new DataView<>("items", filteredDataProvider) {
            @Override
            protected void populateItem(Item<ApiResponseEntry> item) {
                ApiResponseEntry entry = item.getModelObject();
                for (String key : response.getHeader()) {
                    if (key.endsWith("_label") || key.endsWith("_label_multi")) continue;
                    String value = entry.get(key);
                    if (value == null || value.isBlank()) continue;
                    String entryLabel = entry.get(key + "_label");

                    if (key.endsWith("user_iri")) {
                        IRI userIri = Utils.vf.createIRI(value);
                        IRI profilePicIri = User.getProfilePicture(userIri);
                        String imgSrc;
                        String iconClass;
                        if (profilePicIri != null) {
                            imgSrc = Strings.escapeMarkup(profilePicIri.stringValue()).toString();
                            iconClass = "user-icon";
                        } else if (IndividualAgent.isSoftware(userIri)) {
                            imgSrc = RequestCycle.get().urlFor(new ContextRelativeResourceReference("images/bot-icon.svg", false), null).toString();
                            iconClass = "bot-icon";
                        } else {
                            imgSrc = RequestCycle.get().urlFor(new ContextRelativeResourceReference("images/user-icon.svg", false), null).toString();
                            iconClass = "user-icon";
                        }
                        String displayLabel = (entryLabel != null && !entryLabel.isBlank()) ? entryLabel : User.getShortDisplayName(userIri);
                        String userUrl = UserPage.MOUNT_PATH + "?id=" + Utils.urlEncode(value);
                        String html = "<img class=\"" + iconClass + "\" src=\"" + imgSrc + "\" /> <a href=\"" + Strings.escapeMarkup(userUrl) + "\">" + Strings.escapeMarkup(displayLabel) + "</a>";
                        item.add(new Label("listItem", html).setEscapeModelStrings(false));
                        return;
                    } else if (key.endsWith("template_iri")) {
                        String displayLabel = (entryLabel != null && !entryLabel.isBlank()) ? entryLabel : value;
                        String templateUrl = PublishPage.MOUNT_PATH + "?template=" + Utils.urlEncode(value) + "&template-version=latest";
                        String html = "<span class=\"form-icon\"></span> <a href=\"" + Strings.escapeMarkup(templateUrl) + "\">" + Strings.escapeMarkup(displayLabel) + "</a>";
                        item.add(new Label("listItem", html).setEscapeModelStrings(false));
                        return;
                    } else if (value.matches("https?://.*")) {
                        item.add(new NanodashLink("listItem", value, null, null, entryLabel, contextId));
                        return;
                    } else {
                        item.add(new Label("listItem", value));
                        return;
                    }
                }
                item.add(new Label("listItem", ""));
            }
        };
        dataView.setItemsPerPage(viewDisplay.getPageSize());

        WebMarkupContainer navigation = new WebMarkupContainer("navigation");
        navigation.add(new NavigatorLabel("navigatorLabel", dataView));
        AjaxPagingNavigator pagingNavigator = new AjaxPagingNavigator("navigator", dataView);
        navigation.setVisible(dataView.getPageCount() > 1);
        navigation.add(pagingNavigator);

        itemsContainer = new WebMarkupContainer("items-container");
        itemsContainer.setOutputMarkupId(true);
        itemsContainer.add(dataView);
        itemsContainer.add(navigation);
        add(itemsContainer);
    }
}
