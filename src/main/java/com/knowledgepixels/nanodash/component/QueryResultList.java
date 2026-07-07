package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.component.menu.EntryActionMenu;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.NanodashPage;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.page.QueryPage;
import com.knowledgepixels.nanodash.page.UserPage;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.template.Template;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContextRelativeResourceReference;
import org.apache.wicket.util.string.Strings;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Component for displaying query results in a list format.
 */
public class QueryResultList extends QueryResult {

    private static final String SEPARATOR = " · ";

    private FilteredQueryResultDataProvider filteredDataProvider;
    private Model<String> filterModel = Model.of("");
    private WebMarkupContainer itemsContainer;

    /**
     * Constructor for QueryResultList.
     *
     * @param markupId    the markup ID
     * @param queryRef    the query reference
     * @param response    the API response
     * @param viewDisplay the view display
     */
    QueryResultList(String markupId, QueryRef queryRef, ApiResponse response, ViewDisplay viewDisplay) {
        super(markupId, queryRef, response, viewDisplay);

        String label = grlcQuery.getLabel();
        if (viewDisplay.getTitle() != null) {
            label = viewDisplay.getTitle();
        }
        add(new Label("label", label).setVisible(label != null && !label.isEmpty()));
        setOutputMarkupId(true);

        TextField<String> filterField = new TextField<>("filter", filterModel);
        filterField.setOutputMarkupId(true);
        filterField.add(new FilterUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (filteredDataProvider != null && itemsContainer != null) {
                    filteredDataProvider.setFilterText(filterModel.getObject());
                    target.add(itemsContainer);
                }
            }
        });
        filterField.setVisible(!fitsOnFirstPage());
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
                RepeatingView listItem = new RepeatingView("listItem");

                // Columns that only feed action query mappings carry action data, not
                // row content — don't render them as visible row text.
                View viewForColumns = viewDisplay.getView();
                Set<String> hiddenColumns = viewForColumns != null
                        ? viewForColumns.getActionMappingSourceColumns() : Collections.emptySet();
                List<Component> components = new ArrayList<>();
                // The row's "^" source link, if any — folded into the per-row actions
                // dropdown appended at the end of the row.
                String sourceUri = null;
                for (String key : response.getHeader()) {
                    if (key.endsWith("_label") || key.endsWith("_label_multi") || hiddenColumns.contains(key)) {
                        continue;
                    }
                    String entryValue = entry.get(key);
                    if (entryValue != null && !entryValue.isBlank()) {
                        if (key.endsWith("user_iri")) {
                            IRI userIri = Utils.vf.createIRI(entryValue);
                            IRI profilePicIri = User.getProfilePicture(userIri);
                            String imgSrc;
                            if (profilePicIri != null) {
                                imgSrc = Strings.escapeMarkup(profilePicIri.stringValue()).toString();
                            } else if (IndividualAgent.isSoftware(userIri)) {
                                imgSrc = RequestCycle.get().urlFor(new ContextRelativeResourceReference("images/bot-icon.svg", false), null).toString();
                            } else {
                                imgSrc = RequestCycle.get().urlFor(new ContextRelativeResourceReference("images/user-icon.svg", false), null).toString();
                            }
                            String userLabel = entry.get(key + "_label");
                            String displayLabel = userLabel != null && !userLabel.isBlank() ? userLabel : User.getShortDisplayName(userIri);
                            String userUrl = UserPage.MOUNT_PATH + "?id=" + Utils.urlEncode(entryValue);
                            String linkHtml = "<a href=\"" + Strings.escapeMarkup(userUrl) + "\">" + Strings.escapeMarkup(displayLabel) + "</a>";
                            components.add(new ComponentSequence("component", " ", List.of(
                                    new Label("component", "<img class=\"user-icon\" src=\"" + imgSrc + "\" />").setEscapeModelStrings(false),
                                    new Label("component", linkHtml).setEscapeModelStrings(false))));
                        } else if (key.endsWith("template_iri")) {
                            String templateLabel = entry.get(key + "_label");
                            String displayLabel = templateLabel != null && !templateLabel.isBlank() ? templateLabel : entryValue;
                            String templateUrl = PublishPage.MOUNT_PATH + "?template=" + Utils.urlEncode(entryValue) + "&template-version=latest" + templateLinkContextParam();
                            String linkHtml = "<a href=\"" + Strings.escapeMarkup(templateUrl) + "\">" + Strings.escapeMarkup(displayLabel) + "</a>";
                            components.add(new ComponentSequence("component", " ", List.of(
                                    new Label("component", "<span class=\"form-icon\"></span>").setEscapeModelStrings(false),
                                    new Label("component", linkHtml).setEscapeModelStrings(false))));
                        } else if (key.endsWith("query_iri")) {
                            String queryLabel = entry.get(key + "_label");
                            String displayLabel = queryLabel != null && !queryLabel.isBlank() ? queryLabel : entryValue;
                            String queryUrl = QueryPage.MOUNT_PATH + "?id=" + Utils.urlEncode(entryValue) + templateLinkContextParam();
                            String linkHtml = "<a href=\"" + Strings.escapeMarkup(queryUrl) + "\">" + Strings.escapeMarkup(displayLabel) + "</a>";
                            components.add(new Label("component", linkHtml).setEscapeModelStrings(false));
                        } else if (key.endsWith("_multi_iri")) {
                            String[] uris = entryValue.split("\\s+");
                            String labelKey = key.substring(0, key.length() - "_multi_iri".length()) + "_label_multi";
                            String labelValue = entry.get(labelKey);
                            String[] labels = labelValue != null ? labelValue.split("\n", -1) : null;
                            List<Component> links = new ArrayList<>();
                            for (int i = 0; i < uris.length; i++) {
                                String uri = uris[i];
                                if (uri.isBlank()) continue;
                                String label = (labels != null && i < labels.length && !labels[i].isBlank()) ? Utils.unescapeMultiValue(labels[i]) : null;
                                // SPARQL coalesce often falls back to the URI string itself; treat that as no label
                                // so NanodashLink can derive a short name from the URI.
                                if (label != null && label.equals(uri)) label = null;
                                links.add(new NanodashLink("component", uri, null, null, label, contextId));
                            }
                            components.add(new ComponentSequence("component", ", ", links));
                        } else if (key.endsWith("_multi_val")) {
                            String labelKey = key.substring(0, key.length() - "_multi_val".length()) + "_label_multi";
                            String labelValue = entry.get(labelKey);
                            String[] parts = entryValue.split("\n", -1);
                            String[] labels = labelValue != null ? labelValue.split("\n", -1) : null;
                            List<Component> multiComponents = new ArrayList<>();
                            for (int i = 0; i < parts.length; i++) {
                                String part = parts[i];
                                String label = (labels != null && i < labels.length && !labels[i].isBlank()) ? Utils.unescapeMultiValue(labels[i]) : null;
                                if (part.matches("https?://.+")) {
                                    if (label != null && label.equals(part)) label = null;
                                    multiComponents.add(new NanodashLink("component", part, null, null, label, contextId));
                                } else {
                                    String unescaped = Utils.unescapeMultiValue(part);
                                    if (label == null && Utils.isDateTimeLiteral(unescaped)) {
                                        // Friendly relative time, matching single-value items.
                                        multiComponents.add(new Label("component", Utils.friendlyDateHtml(unescaped, unescaped)).setEscapeModelStrings(false));
                                    } else {
                                        String display = label != null ? label : unescaped;
                                        boolean isHtml = Utils.looksLikeHtml(display);
                                        if (isHtml) {
                                            display = withContextInHtmlLinks(Utils.sanitizeHtml(display));
                                        }
                                        multiComponents.add(new Label("component", display).setEscapeModelStrings(!isHtml));
                                    }
                                }
                            }
                            components.add(new ComponentSequence("component", ", ", multiComponents));
                        } else if (key.endsWith("_multi")) {
                            String[] parts = entryValue.split("\n", -1);
                            String labelKey = key.substring(0, key.length() - "_multi".length()) + "_label_multi";
                            String labelValue = entry.get(labelKey);
                            String[] labels = labelValue != null ? labelValue.split("\n", -1) : null;
                            List<Component> multiComponents = new ArrayList<>();
                            for (int i = 0; i < parts.length; i++) {
                                boolean hasLabel = labels != null && i < labels.length && !labels[i].isBlank();
                                String display = hasLabel ? Utils.unescapeMultiValue(labels[i]) : Utils.unescapeMultiValue(parts[i]);
                                if (!hasLabel && Utils.isDateTimeLiteral(display)) {
                                    // Friendly relative time, matching single-value items.
                                    multiComponents.add(new Label("component", Utils.friendlyDateHtml(display, display)).setEscapeModelStrings(false));
                                } else {
                                    multiComponents.add(new Label("component", display));
                                }
                            }
                            components.add(new ComponentSequence("component", ", ", multiComponents));
                        } else if (entryValue.matches("https?://.+")) {
                            String entryLabel = entry.get(key + "_label");
                            if ("^".equals(entryLabel)) {
                                // Folded into the per-row actions dropdown appended below.
                                sourceUri = entryValue;
                            } else {
                                components.add(new NanodashLink("component", entryValue, null, null, entryLabel, contextId));
                            }
                        } else {
                            String entryLabel = entry.get(key + "_label");
                            boolean hasLabel = entryLabel != null && !entryLabel.isBlank() && !entryLabel.equals(entryValue);
                            if (!hasLabel && Utils.isDateTimeLiteral(entryValue)) {
                                // Show a friendly relative time (client-side); raw ISO value stays as no-script fallback.
                                components.add(new Label("component", Utils.friendlyDateHtml(entryValue, entryValue)).setEscapeModelStrings(false));
                            } else {
                                String display = hasLabel ? entryLabel : entryValue;
                                if (Utils.looksLikeHtml(display)) {
                                    display = withContextInHtmlLinks(Utils.sanitizeHtml(display));
                                } else if (hasLabel) {
                                    display = Strings.escapeMarkup(display).toString();
                                }
                                if (hasLabel) {
                                    // Separate display label for a (non-IRI) literal value; the full
                                    // literal is shown on hover via the standard styled tooltip.
                                    display = "<span class=\"tooltip\"><span class=\"tooltiptext tooltiptext-auto\">" + Strings.escapeMarkup(entryValue) + "</span>" + display + "</span>";
                                }
                                components.add(new Label("component", display).setEscapeModelStrings(false));
                            }
                        }
                    }
                }
                View view = viewDisplay.getView();
                List<AbstractLink> actionLinks = new ArrayList<>();
                if (view != null) {
                    for (IRI actionIri : view.getViewEntryActionList()) {
                        // Per-action role gating (docs/role-specific-views.md): skip an
                        // action whose gen:isVisibleTo the viewer does not satisfy.
                        if (!SpaceMemberRole.isViewerEntitled(view.getActionVisibleTo(actionIri), resourceWithProfile, refRoot)) continue;
                        // TODO Copied code and adjusted from QueryResultTableBuilder:
                        Template t = view.getTemplateForAction(actionIri);
                        if (t == null) continue;
                        String targetField = view.getTemplateTargetFieldForAction(actionIri);
                        if (targetField == null) targetField = "resource";
                        String labelForAction = view.getLabelForAction(actionIri);
                        if (labelForAction == null) labelForAction = "action...";
                        if (!labelForAction.endsWith("...")) labelForAction += "...";
                        PageParameters params = new PageParameters().set("template", t.getId())
                                .set("param_" + targetField, contextId)
                                .set("context", contextId)
                                .set("template-version", "latest");
                        String partField = view.getTemplatePartFieldForAction(actionIri);
                        if (partField != null) {
                            // TODO Find a better way to pass the MaintainedResource object to this method:
                            MaintainedResource r = MaintainedResourceRepository.get().findById(contextId);
                            if (r != null && r.getNamespace() != null) {
                                params.set("param_" + partField, r.getNamespace() + "<SET-SUFFIX>");
                            }
                        }
                        // Apply the action's query mappings; hide the button for this row
                        // if any required mapped value is empty (docs/magic-query-params.md).
                        if (!ViewActionMappings.applyEntryMappings(view, actionIri, entry, params)) {
                            continue;
                        }
                        params.set("refresh-upon-publish", queryRef.getAsUrlString());
                        if (postPublishTab != null) params.set("postpub-tab", postPublishTab);
                        AbstractLink button = new BookmarkablePageLink<NanodashPage>("link", PublishPage.class, params);
                        // A label that starts with a leading symbol/emoji renders that as the entry icon.
                        String iconBody = Utils.menuEntryIconBodyHtml(labelForAction);
                        if (iconBody != null) {
                            button.setBody(Model.of(iconBody)).setEscapeModelStrings(false);
                        } else {
                            button.setBody(Model.of(labelForAction));
                        }
                        actionLinks.add(button);
                    }
                }
                // The former "^" source link joins the same dropdown, as a "source" entry.
                if (sourceUri != null) {
                    AbstractLink sourceLink = new BookmarkablePageLink<NanodashPage>("link", ExplorePage.class,
                            new PageParameters().set("id", sourceUri));
                    sourceLink.add(NavigationContext.pageContextFallback());
                    sourceLink.setBody(Model.of("<span class=\"actionmenu-icon\">↗︎</span>source")).setEscapeModelStrings(false);
                    actionLinks.add(sourceLink);
                }
                // Append the per-row dropdown (entry actions + source) as the trailing item;
                // it hugs the preceding content with a plain space, not the list separator.
                Set<Integer> spaceBeforeMenu = new HashSet<>();
                if (!actionLinks.isEmpty()) {
                    spaceBeforeMenu.add(components.size());
                    components.add(new EntryActionMenu("component", actionLinks));
                }
                ComponentSequence componentSequence = new ComponentSequence(listItem.newChildId(), SEPARATOR, components, spaceBeforeMenu);
                listItem.add(componentSequence);
                item.add(listItem);
            }
        };
        dataView.setItemsPerPage(viewDisplay.getPageSize());

        WebMarkupContainer navigation = new WebMarkupContainer("navigation");
        navigation.add(new NavigatorLabel("navigatorLabel", dataView));
        AjaxPagingNavigator pagingNavigator = new AjaxPagingNavigator("navigator", dataView);
        navigation.setVisible(dataView.getPageCount() > 1);
        navigation.add(pagingNavigator);

        // Hidden when the empty-actions line below shows instead, which carries
        // its own "Nothing here yet:" text; this note still covers the case of
        // the filter text matching no row.
        Label noRecordsLabel = new Label("no-records", "(nothing found)") {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(filteredDataProvider.size() == 0 && !hasEmptyStateActions());
            }
        };

        // When the result is genuinely empty (not merely filtered down to zero
        // rows), the view-level actions are promoted from the dropdown menu to
        // visible buttons in the empty state — same as in QueryResultTable.
        WebMarkupContainer emptyActions = new WebMarkupContainer("empty-actions") {
            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(hasEmptyStateActions());
            }
        };
        // menuActions is filled by the builder after construction, so the list
        // is wrapped as a live model rather than copied here.
        emptyActions.add(new ListView<MenuAction>("actions", new ListModel<>(menuActions)) {
            @Override
            protected void populateItem(ListItem<MenuAction> item) {
                MenuAction action = item.getModelObject();
                AbstractLink link = new BookmarkablePageLink<NanodashPage>("link", action.pageClass(), action.params());
                link.setBody(Model.of(action.label()));
                item.add(link);
            }
        });

        itemsContainer = new WebMarkupContainer("items-container");
        itemsContainer.setOutputMarkupId(true);
        itemsContainer.add(dataView);
        itemsContainer.add(noRecordsLabel);
        itemsContainer.add(emptyActions);
        itemsContainer.add(navigation);
        add(itemsContainer);
    }

}
