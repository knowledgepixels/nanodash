package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
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
import java.util.List;

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
            // The last data column (ignoring _label helper columns); if it is the
            // source-nanopub column ("np"/"nps") its header is left blank.
            String lastColumnKey = null;
            for (String h : response.getHeader()) {
                if (h.endsWith("_label") || h.endsWith("_label_multi")) continue;
                lastColumnKey = h;
            }
            for (String h : response.getHeader()) {
                if (h.endsWith("_label") || h.endsWith("_label_multi")) {
                    continue;
                }
                String displayLabel = h;
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
                if (h.equals(lastColumnKey) && (h.equals("np") || h.equals("nps"))) {
                    columnHeader = "";
                    columns.add(new Column(columnHeader, h, "cell-right"));
                } else {
                    columns.add(new Column(columnHeader, h));
                }
            }
            if (viewDisplay.getView() != null && !viewDisplay.getView().getViewEntryActionList().isEmpty()) {
                columns.add(new Column("", Column.ACTIONS));
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
            table.addBottomToolbar(new AjaxNavigationToolbar(table));
            table.addTopToolbar(new AjaxFallbackHeadersToolbar<String>(table, dataProvider));
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
        private String cssClass;
        public static final String ACTIONS = "*actions*";

        public Column(String title, String key) {
            this(title, key, null);
        }

        public Column(String title, String key, String cssClass) {
            super(new Model<String>(title), key);
            this.key = key;
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
                if (key.equals(ACTIONS) && view != null) {
                    List<AbstractLink> links = new ArrayList<>();
                    for (IRI actionIri : view.getViewEntryActionList()) {
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
                        String queryMapping = view.getTemplateQueryMapping(actionIri);
                        if (queryMapping != null && queryMapping.contains(":")) {
                            // This part is different from the code in QueryResultTableBuilder:
                            String queryParam = queryMapping.split(":")[0];
                            String templateParam = queryMapping.split(":")[1];
                            params.set("param_" + templateParam, rowModel.getObject().get(queryParam));
                        }
                        params.set("refresh-upon-publish", queryRef.getAsUrlString());
                        AbstractLink button = new BookmarkablePageLink<NanodashPage>("button", PublishPage.class, params);
                        button.setBody(Model.of(label));
                        links.add(button);
                    }
                    cellItem.add(new ButtonList(componentId, resourceWithProfile, links, null, null));
                } else {
                    String value = rowModel.getObject().get(key);
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
