package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.component.menu.EntryActionMenu;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.NanodashPage;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.template.Template;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackHeadersToolbar;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxNavigationToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.*;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Component for displaying query results in a table format.
 */
public class QueryResultTable extends QueryResult {

    private static final Logger logger = LoggerFactory.getLogger(QueryResultTable.class);

    private Model<String> errorMessages = Model.of("");
    private DataTable<ApiResponseEntry, String> table;
    private Label noRecordsLabel;
    private Label errorLabel;
    private FilteredQueryResultDataProvider filteredDataProvider;
    private Model<String> filterModel = Model.of("");
    // The source-nanopub column ("np"/"nps"), folded into the per-row actions dropdown.
    private String sourceColumnKey;

    QueryResultTable(String id, QueryRef queryRef, ApiResponse response, ViewDisplay viewDisplay, boolean plain) {
        super(id, queryRef, response, viewDisplay);

        if (plain) {
            add(new Label("label").setVisible(false));
            add(new Label("np").setVisible(false));
            showViewDisplayMenu = false;
        } else {
            String label = grlcQuery.getLabel();
            if (viewDisplay.getTitle() != null) {
                label = viewDisplay.getTitle();
            }
            add(new Label("label", label));
        }

        errorLabel = new Label("error-messages", errorMessages);
        errorLabel.setVisible(false);
        add(errorLabel);

        TextField<String> filterField = new TextField<>("filter", filterModel);
        filterField.setOutputMarkupId(true);
        filterField.add(new FilterUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (filteredDataProvider != null && table != null) {
                    filteredDataProvider.setFilterText(filterModel.getObject());
                    target.add(table);
                    if (noRecordsLabel != null) target.add(noRecordsLabel);
                }
            }
        });
        filterField.setVisible(!fitsOnFirstPage());
        add(filterField);

        populateComponent();
    }

    private void addErrorMessage(String errorMessage) {
        String s = errorMessages.getObject();
        if (s.isEmpty()) {
            s = "Error: " + errorMessage;
        } else {
            s += ", " + errorMessage;
        }
        errorMessages.setObject(s);
        errorLabel.setVisible(true);
        if (table != null) table.setVisible(false);
    }

    @Override
    protected void populateComponent() {
        List<IColumn<ApiResponseEntry, String>> columns = new ArrayList<>();
        QueryResultDataProvider dataProvider;
        try {
            // Columns that only feed action query mappings (conditional targets, the
            // local-key bundle) carry action data, not row content — don't render them.
            Set<String> hiddenColumns = viewDisplay.getView() != null
                    ? viewDisplay.getView().getActionMappingSourceColumns() : Collections.emptySet();
            // The source-nanopub column ("np"/"nps") is no longer rendered as its own
            // column; it becomes the "source" entry of this row's actions dropdown.
            sourceColumnKey = null;
            for (String h : response.getHeader()) {
                if (h.equals("np") || h.equals("nps")) sourceColumnKey = h;
            }
            // Whether any rendered column carries a visible header label. If none do
            // (every column is "_noheader"), the entire header row is dropped — see the
            // gated addTopToolbar below.
            boolean anyHeaderShown = false;
            for (String h : response.getHeader()) {
                if (h.endsWith("_label") || h.endsWith("_label_multi")
                        || hiddenColumns.contains(h) || h.equals(sourceColumnKey)) {
                    continue;
                }
                // A trailing "_noheader" hides this column's header label while still
                // rendering the column. It is stripped first to recover the logical
                // column key, so every other rule (type suffix, _label companion,
                // action mappings) operates unchanged on the unmarked name.
                boolean noHeader = h.endsWith("_noheader");
                String key = noHeader ? h.substring(0, h.length() - "_noheader".length()) : h;
                String displayLabel = key;
                if (displayLabel.endsWith("_multi_iri")) {
                    displayLabel = displayLabel.substring(0, displayLabel.length() - "_multi_iri".length());
                } else if (displayLabel.endsWith("_multi_val")) {
                    displayLabel = displayLabel.substring(0, displayLabel.length() - "_multi_val".length());
                } else if (displayLabel.endsWith("_multi")) {
                    displayLabel = displayLabel.substring(0, displayLabel.length() - "_multi".length());
                } else if (displayLabel.endsWith("_iri")) {
                    displayLabel = displayLabel.substring(0, displayLabel.length() - "_iri".length());
                }
                String columnHeader = displayLabel.replaceAll("_", " ");
                if (noHeader) {
                    columns.add(new Column("", h, key, null));
                } else {
                    anyHeaderShown = true;
                    columns.add(new Column(columnHeader, h, key, null));
                }
            }
            // A single trailing dropdown column bundling this row's entry-level actions
            // and its "source" link, shown whenever either is present.
            boolean hasEntryActions = viewDisplay.getView() != null
                    && !viewDisplay.getView().getViewEntryActionList().isEmpty();
            if (hasEntryActions || sourceColumnKey != null) {
                columns.add(new Column("", Column.ACTIONS, "cell-right"));
            }
            dataProvider = new QueryResultDataProvider(response.getData());
            filteredDataProvider = new FilteredQueryResultDataProvider(dataProvider, response);
            // The whole table (header included) is hidden when there is nothing to show;
            // a "(nothing found)" note is shown instead. No NoRecordsToolbar, since that
            // would leave the header row visible.
            table = new DataTable<>("table", columns, filteredDataProvider, viewDisplay.getPageSize() < 1 ? Integer.MAX_VALUE : viewDisplay.getPageSize()) {
                @Override
                protected void onConfigure() {
                    super.onConfigure();
                    setVisible(errorMessages.getObject().isEmpty() && filteredDataProvider.size() > 0);
                }
            };
            table.setOutputMarkupPlaceholderTag(true);
            // Marker class so nanodash.js can wrap emoji in body cells with the same
            // monochrome .emoji styling used for headings (e.g. the ✅/⚠️ key-approval
            // annotations in the "keys" column).
            table.add(new AttributeAppender("class", "result-table"));
            table.addBottomToolbar(new AjaxNavigationToolbar(table));
            // Drop the header row entirely when no column has a visible header label.
            if (anyHeaderShown) {
                table.addTopToolbar(new AjaxFallbackHeadersToolbar<String>(table, dataProvider));
            }
            add(table);
            noRecordsLabel = new Label("no-records", "(nothing found)") {
                @Override
                protected void onConfigure() {
                    super.onConfigure();
                    setVisible(errorMessages.getObject().isEmpty() && filteredDataProvider.size() == 0);
                }
            };
            noRecordsLabel.setOutputMarkupPlaceholderTag(true);
            add(noRecordsLabel);
        } catch (Exception ex) {
            logger.error("Error creating table for query {}", grlcQuery.getQueryId(), ex);
            add(new Label("table", "").setVisible(false));
            add(new Label("no-records", "").setVisible(false));
            addErrorMessage(ex.getMessage());
        }
    }

    private class Column extends AbstractColumn<ApiResponseEntry, String> implements IStyledColumn<ApiResponseEntry, String> {

        private String key;
        // The actual response-column name to read row data from. Differs from the
        // logical key only for "_noheader" columns, whose marker is kept here but
        // stripped from key so all name-matching uses the unmarked name.
        private String dataKey;
        private String cssClass;
        public static final String ACTIONS = "*actions*";

        public Column(String title, String key) {
            this(title, key, key, null);
        }

        public Column(String title, String key, String cssClass) {
            this(title, key, key, cssClass);
        }

        public Column(String title, String dataKey, String key, String cssClass) {
            super(new Model<String>(title), dataKey);
            this.key = key;
            this.dataKey = dataKey;
            this.cssClass = cssClass;
        }

        @Override
        public String getCssClass() {
            return cssClass;
        }

        @Override
        public void populateItem(Item<ICellPopulator<ApiResponseEntry>> cellItem, String componentId, IModel<ApiResponseEntry> rowModel) {
            try {
                View view = viewDisplay.getView();
                if (key.equals(ACTIONS)) {
                    List<AbstractLink> links = new ArrayList<>();
                    if (view != null) {
                        for (IRI actionIri : view.getViewEntryActionList()) {
                            // Per-action role gating (docs/role-specific-views.md): skip an
                            // action whose gen:isVisibleTo the viewer does not satisfy.
                            // Additive — actions without gen:isVisibleTo are unaffected.
                            if (!SpaceMemberRole.isViewerEntitled(view.getActionVisibleTo(actionIri), resourceWithProfile)) continue;
                            // TODO Copied code and adjusted from QueryResultTableBuilder:
                            Template t = view.getTemplateForAction(actionIri);
                            if (t == null) continue;
                            String targetField = view.getTemplateTargetFieldForAction(actionIri);
                            if (targetField == null) targetField = "resource";
                            String label = view.getLabelForAction(actionIri);
                            if (label == null) label = "action...";
                            if (!label.endsWith("...")) label += "...";
                            PageParameters params = new PageParameters().set("template", t.getId())
                                    .set("param_" + targetField, contextId)
                                    .set("context", contextId)
                                    .set("template-version", "latest");
                            if (partId != null && contextId != null && !partId.equals(contextId)) {
                                params.set("part", partId);
                            }
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
                            if (!ViewActionMappings.applyEntryMappings(view, actionIri, rowModel.getObject(), params)) {
                                continue;
                            }
                            params.set("refresh-upon-publish", queryRef.getAsUrlString());
                            if (postPublishTab != null) params.set("postpub-tab", postPublishTab);
                            AbstractLink button = new BookmarkablePageLink<NanodashPage>("link", PublishPage.class, params);
                            button.setBody(Model.of(label));
                            links.add(button);
                        }
                    }
                    // The former "^" source link joins the same dropdown, as a "source" entry.
                    if (sourceColumnKey != null) {
                        String sourceUri = rowModel.getObject().get(sourceColumnKey);
                        if (sourceUri != null && !sourceUri.isBlank()) {
                            AbstractLink sourceLink = new BookmarkablePageLink<NanodashPage>("link", ExplorePage.class,
                                    new PageParameters().set("id", sourceUri));
                            sourceLink.setBody(Model.of("source"));
                            links.add(sourceLink);
                        }
                    }
                    if (links.isEmpty()) {
                        cellItem.add(new Label(componentId).setVisible(false));
                    } else {
                        cellItem.add(new EntryActionMenu(componentId, links));
                    }
                } else {
                    String value = rowModel.getObject().get(dataKey);
                    if (key.endsWith("_multi_iri")) {
                        String labelKey = key.substring(0, key.length() - "_multi_iri".length()) + "_label_multi";
                        String labelValue = rowModel.getObject().get(labelKey);
                        String[] uris = (value == null || value.isBlank()) ? new String[0] : value.split("\\s+");
                        String[] labels = labelValue != null ? labelValue.split("\n", -1) : null;
                        List<Component> links = new ArrayList<>();
                        for (int i = 0; i < uris.length; i++) {
                            String uri = uris[i];
                            if (uri.isBlank()) continue;
                            String rawLabel = (labels != null && i < labels.length && !labels[i].isBlank()) ? Utils.unescapeMultiValue(labels[i]) : null;
                            // SPARQL coalesce often falls back to the URI string itself; treat that as no label
                            // so NanodashLink can derive a short name from the URI.
                            if (rawLabel != null && rawLabel.equals(uri)) rawLabel = null;
                            String label = truncateLabel(rawLabel);
                            links.add(new NanodashLink("component", uri, null, null, label, contextId));
                        }
                        cellItem.add(new ComponentSequence(componentId, ", ", links));
                    } else if (key.endsWith("_multi_val")) {
                        String labelKey = key.substring(0, key.length() - "_multi_val".length()) + "_label_multi";
                        String labelValue = rowModel.getObject().get(labelKey);
                        String[] parts = (value == null) ? new String[0] : value.split("\n", -1);
                        String[] labels = labelValue != null ? labelValue.split("\n", -1) : null;
                        List<Component> components = new ArrayList<>();
                        for (int i = 0; i < parts.length; i++) {
                            String part = parts[i];
                            String rawLabel = (labels != null && i < labels.length && !labels[i].isBlank()) ? Utils.unescapeMultiValue(labels[i]) : null;
                            if (part.matches("https?://.+")) {
                                if (rawLabel != null && rawLabel.equals(part)) rawLabel = null;
                                String label = truncateLabel(rawLabel);
                                components.add(new NanodashLink("component", part, null, null, label, contextId));
                            } else {
                                String label = truncateLabel(rawLabel);
                                String display = label != null ? label : Utils.unescapeMultiValue(part);
                                if (Utils.looksLikeHtml(display)) {
                                    components.add(new Label("component", Utils.sanitizeHtml(display))
                                            .setEscapeModelStrings(false)
                                            .add(new AttributeAppender("class", "cell-data-html")));
                                } else {
                                    components.add(new Label("component", display));
                                }
                            }
                        }
                        cellItem.add(new ComponentSequence(componentId, ", ", components));
                    } else if (key.endsWith("_multi")) {
                        String labelKey = key.substring(0, key.length() - "_multi".length()) + "_label_multi";
                        String labelValue = rowModel.getObject().get(labelKey);
                        String[] parts = (value == null) ? new String[0] : value.split("\n", -1);
                        String[] labels = labelValue != null ? labelValue.split("\n", -1) : null;
                        List<Component> components = new ArrayList<>();
                        for (int i = 0; i < parts.length; i++) {
                            String display;
                            if (labels != null && i < labels.length && !labels[i].isBlank()) {
                                display = Utils.unescapeMultiValue(labels[i]);
                            } else {
                                display = Utils.unescapeMultiValue(parts[i]);
                            }
                            if (Utils.looksLikeHtml(display)) {
                                components.add(new Label("component", Utils.sanitizeHtml(display))
                                        .setEscapeModelStrings(false)
                                        .add(new AttributeAppender("class", "cell-data-html")));
                            } else {
                                components.add(new Label("component", display));
                            }
                        }
                        cellItem.add(new ComponentSequence(componentId, ", ", components));
                    } else if (key.endsWith("template_iri")) {
                        String label = truncateLabel(rowModel.getObject().get(key + "_label"));
                        if (label == null || label.isBlank()) label = truncateLabel(value);
                        String templateUrl = PublishPage.MOUNT_PATH + "?template=" + Utils.urlEncode(value) + "&template-version=latest";
                        String html = "<a href=\"" + Strings.escapeMarkup(templateUrl) + "\">" + Strings.escapeMarkup(label) + "</a>";
                        cellItem.add(new Label(componentId, html).setEscapeModelStrings(false));
                    } else if (value.matches("https?://.+")) {
                        String label = truncateLabel(rowModel.getObject().get(key + "_label"));
                        cellItem.add(new NanodashLink(componentId, value, null, null, label, contextId));
                    } else {
                        if (key.startsWith("pubkey")) {
                            cellItem.add(new Label(componentId, value).add(new AttributeAppender("style", "overflow-wrap: anywhere;")));
                        } else if (Utils.isDateTimeLiteral(value)) {
                            // Show a friendly relative time (client-side); raw ISO value stays as no-script fallback.
                            cellItem.add(new Label(componentId, Utils.friendlyDateHtml(value, value)).setEscapeModelStrings(false));
                        } else {
                            Label cellLabel;
                            if (Utils.looksLikeHtml(value)) {
                                cellLabel = (Label) new Label(componentId, Utils.sanitizeHtml(value))
                                        .setEscapeModelStrings(false)
                                        .add(new AttributeAppender("class", "cell-data-html"));
                            } else {
                                cellLabel = new Label(componentId, value);
                            }
                            cellItem.add(cellLabel);
                        }
                    }
                }
            } catch (Exception ex) {
                logger.error("Failed to populate table column: ", ex);
                cellItem.add(new Label(componentId).setVisible(false));
                addErrorMessage(ex.getMessage());
            }
        }

    }

    private static String truncateLabel(String label) {
        return Utils.truncateLabel(label);
    }

}
