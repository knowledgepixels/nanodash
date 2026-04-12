package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.page.NanodashPage;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.template.Template;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NavigatorLabel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContextRelativeResourceReference;
import org.apache.wicket.util.string.Strings;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Component for displaying query results in a list format.
 */
public class QueryResultList extends QueryResult {

    private static final String SEPARATOR = " · ";

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
        add(new Label("label", label));
        setOutputMarkupId(true);
        populateComponent();
    }

    @Override
    protected void populateComponent() {
        QueryResultDataProvider dataProvider = new QueryResultDataProvider(response.getData());
        DataView<ApiResponseEntry> dataView = new DataView<>("items", dataProvider) {

            @Override
            protected void populateItem(Item<ApiResponseEntry> item) {
                ApiResponseEntry entry = item.getModelObject();
                RepeatingView listItem = new RepeatingView("listItem");

                List<Component> components = new ArrayList<>();
                for (String key : response.getHeader()) {
                    if (key.endsWith("_label") || key.endsWith("_label_multi")) {
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
                            components.add(new ComponentSequence("component", " ", List.of(
                                    new Label("component", "<img class=\"user-icon\" src=\"" + imgSrc + "\" />").setEscapeModelStrings(false),
                                    new NanodashLink("component", entryValue, null, null, entry.get(key + "_label"), contextId))));
                        } else if (key.endsWith("template_iri")) {
                            String templateLabel = entry.get(key + "_label");
                            String displayLabel = templateLabel != null && !templateLabel.isBlank() ? templateLabel : entryValue;
                            String templateUrl = PublishPage.MOUNT_PATH + "?template=" + Utils.urlEncode(entryValue) + "&template-version=latest";
                            ExternalLink templateLink = new ExternalLink("component", templateUrl, displayLabel);
                            components.add(new ComponentSequence("component", " ", List.of(
                                    new Label("component", "<span class=\"form-icon\"></span>").setEscapeModelStrings(false),
                                    templateLink)));
                        } else if (key.endsWith("_multi_iri")) {
                            String[] uris = entryValue.split("\\s+");
                            String labelKey = key.substring(0, key.length() - "_multi_iri".length()) + "_label_multi";
                            String labelValue = entry.get(labelKey);
                            String[] labels = labelValue != null ? labelValue.split("\n", -1) : null;
                            List<Component> links = new ArrayList<>();
                            for (int i = 0; i < uris.length; i++) {
                                String label = (labels != null && i < labels.length && !labels[i].isBlank()) ? Utils.unescapeMultiValue(labels[i]) : null;
                                links.add(new NanodashLink("component", uris[i], null, null, label, contextId));
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
                                    multiComponents.add(new NanodashLink("component", part, null, null, label, contextId));
                                } else {
                                    String display = label != null ? label : Utils.unescapeMultiValue(part);
                                    boolean isHtml = Utils.looksLikeHtml(display);
                                    if (isHtml) {
                                        display = Utils.sanitizeHtml(display);
                                    }
                                    multiComponents.add(new Label("component", display).setEscapeModelStrings(!isHtml));
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
                                String display;
                                if (labels != null && i < labels.length && !labels[i].isBlank()) {
                                    display = Utils.unescapeMultiValue(labels[i]);
                                } else {
                                    display = Utils.unescapeMultiValue(parts[i]);
                                }
                                multiComponents.add(new Label("component", display));
                            }
                            components.add(new ComponentSequence("component", ", ", multiComponents));
                        } else if (entryValue.matches("https?://.+")) {
                            String entryLabel = entry.get(key + "_label");
                            components.add(new NanodashLink("component", entryValue, null, null, entryLabel, contextId));
                        } else {
                            if (Utils.looksLikeHtml(entryValue)) {
                                entryValue = Utils.sanitizeHtml(entryValue);
                            }
                            components.add(new Label("component", entryValue).setEscapeModelStrings(false));
                        }
                    }
                }
                View view = viewDisplay.getView();
                if (view != null && !view.getViewEntryActionList().isEmpty()) {
                    List<AbstractLink> links = new ArrayList<>();
                    for (IRI actionIri : view.getViewEntryActionList()) {
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
                        String queryMapping = view.getTemplateQueryMapping(actionIri);
                        if (queryMapping != null && queryMapping.contains(":")) {
                            // This part is different from the code in QueryResultTableBuilder:
                            String queryParam = queryMapping.split(":")[0];
                            String templateParam = queryMapping.split(":")[1];
                            params.set("param_" + templateParam, entry.get(queryParam));
                        }
                        params.set("refresh-upon-publish", queryRef.getAsUrlString());
                        AbstractLink button = new BookmarkablePageLink<NanodashPage>("button", PublishPage.class, params);
                        button.setBody(Model.of(labelForAction));
                        links.add(button);
                    }
                    components.add(new ButtonList("component", resourceWithProfile, links, null, null));
                }
                ComponentSequence componentSequence = new ComponentSequence(listItem.newChildId(), SEPARATOR, components);
                listItem.add(componentSequence);
                item.add(listItem);
            }
        };
        dataView.setItemsPerPage(10);
        dataView.setOutputMarkupId(true);

        WebMarkupContainer navigation = new WebMarkupContainer("navigation");
        navigation.add(new NavigatorLabel("navigatorLabel", dataView));
        AjaxPagingNavigator pagingNavigator = new AjaxPagingNavigator("navigator", dataView);
        navigation.setVisible(dataView.getPageCount() > 1);
        navigation.add(pagingNavigator);

        add(navigation);
        add(dataView);
    }

}
