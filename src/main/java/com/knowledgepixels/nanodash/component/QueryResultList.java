package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.NanodashPage;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.template.Template;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.QueryRef;

import java.util.ArrayList;
import java.util.List;

public class QueryResultList extends Panel {

    private static final String SEPARATOR = ", ";
    private final List<AbstractLink> buttons = new ArrayList<>();
    private String contextId;
    private boolean finalized = false;
    private Space space;

    QueryResultList(String markupId, QueryRef queryRef, ApiResponse response, ViewDisplay viewDisplay) {
        super(markupId);

        add(new AttributeAppender("class", " col-" + viewDisplay.getDisplayWidth()));

        final GrlcQuery grlcQuery = GrlcQuery.get(queryRef);
        String label = grlcQuery.getLabel();
        if (viewDisplay.getView().getTitle() != null) {
            label = viewDisplay.getView().getTitle();
        }
        add(new Label("label", label));
        if (viewDisplay.getNanopubId() != null) {
            add(new BookmarkablePageLink<Void>("np", ExplorePage.class, new PageParameters().set("id", viewDisplay.getNanopubId())));
        } else {
            add(new Label("np").setVisible(false));
        }
        RepeatingView listItems = new RepeatingView("listItems");
        for (ApiResponseEntry entry : response.getData()) {
            List<Component> components = new ArrayList<>();
            for (String key : response.getHeader()) {
                if (!key.endsWith("_label")) {
                    String entryValue = entry.get(key);
                    if (entryValue != null && !entryValue.isBlank()) {
                        if (entryValue.matches("https?://.+")) {
                            String entryLabel = entry.get(key + "_label");
                            components.add(new NanodashLink("component", entryValue, null, null, entryLabel));
                        } else {
                            if (Utils.looksLikeHtml(entryValue)) {
                                entryValue = Utils.sanitizeHtml(entryValue);
                            }
                            components.add(new Label("component", entryValue));
                        }
                    }
                }
            }
            ResourceView view = viewDisplay.getView();
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
                    PageParameters params = new PageParameters().set("template", t.getId()).set("param_" + targetField, contextId).set("context", contextId);
                    String partField = view.getTemplatePartFieldForAction(actionIri);
                    if (partField != null) {
                        // TODO Find a better way to pass the MaintainedResource object to this method:
                        MaintainedResource r = MaintainedResource.get(contextId);
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
                components.add(new ButtonList("component", space, links, null, null));
            }
            ComponentSequence componentSequence = new ComponentSequence(listItems.newChildId(), SEPARATOR, components);
            listItems.add(componentSequence);
        }
        add(listItems);
    }

    public void addButton(String label, Class<? extends NanodashPage> pageClass, PageParameters parameters) {
        if (parameters == null) {
            parameters = new PageParameters();
        }
        if (contextId != null) {
            parameters.set("context", contextId);
        }
        AbstractLink button = new BookmarkablePageLink<NanodashPage>("button", pageClass, parameters);
        button.setBody(Model.of(label));
        buttons.add(button);
    }

    @Override
    protected void onBeforeRender() {
        if (!finalized) {
            if (!buttons.isEmpty()) {
                add(new ButtonList("buttons", space, buttons, null, null));
            } else {
                add(new Label("buttons").setVisible(false));
            }
            finalized = true;
        }
        super.onBeforeRender();
    }

    /**
     * Sets the space.
     *
     * @param space the space
     */
    public void setSpace(Space space) {
        this.space = space;
    }

    /**
     * Sets the context ID.
     *
     * @param contextId the context ID
     */
    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

}
