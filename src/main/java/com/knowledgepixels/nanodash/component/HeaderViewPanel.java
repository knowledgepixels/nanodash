package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.QueryResult;
import com.knowledgepixels.nanodash.SpaceMemberRole;
import com.knowledgepixels.nanodash.View;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.component.menu.ViewDisplayMenu;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.MaintainedResource;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.template.Template;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;

import java.util.ArrayList;
import java.util.List;

/**
 * The body of a header view ({@code gen:HeaderView}, issue #572): a section header
 * at the view's structural position, showing the view's title, an optional
 * description text below it, and the view's result-level actions as top entries of
 * the view-display dropdown menu. Header views have no query, so this panel always
 * renders synchronously.
 */
public class HeaderViewPanel extends Panel {

    /**
     * @param markupId            the Wicket markup id
     * @param viewDisplay         the view display of the header view
     * @param resourceWithProfile the resource whose page the header is on (actions
     *                            need it for visibility gating; may be null)
     * @param id                  the resource or part id the view is shown for
     * @param contextId           the context id, or null
     * @param refRoot             the pinned ref's root nanopub, or null
     */
    public HeaderViewPanel(String markupId, ViewDisplay viewDisplay, AbstractResourceWithProfile resourceWithProfile, String id, String contextId, String refRoot) {
        super(markupId);
        View view = viewDisplay.getView();
        add(new Label("title", viewDisplay.getTitle()));
        String description = view.getDescription();
        add(new Label("description", description).setVisible(description != null && !description.isBlank()));

        // Result-level actions become the top entries of the view-display menu,
        // mirroring how the query-result components route their view-level actions.
        List<QueryResult.MenuAction> menuActions = new ArrayList<>();
        if (resourceWithProfile != null) {
            for (IRI actionIri : view.getViewResultActionList()) {
                if (!SpaceMemberRole.isViewerEntitled(view.getActionVisibleTo(actionIri), resourceWithProfile, refRoot)) continue;
                Template t = view.getTemplateForAction(actionIri);
                if (t == null) continue;
                String targetField = view.getTemplateTargetFieldForAction(actionIri);
                if (targetField == null) targetField = "resource";
                String label = view.getLabelForAction(actionIri);
                if (label == null) label = "action...";
                if (!label.endsWith("...")) label += "...";
                PageParameters params = new PageParameters().set("template", t.getId())
                        .set("param_" + targetField, id)
                        .set("context", contextId)
                        .set("template-version", "latest");
                if (id != null && contextId != null && !id.equals(contextId)) {
                    params.set("part", id);
                }
                String partField = view.getTemplatePartFieldForAction(actionIri);
                if (partField != null && contextId != null) {
                    // The part field pre-fills a namespaced child IRI (the user fills the suffix).
                    MaintainedResource r = MaintainedResourceRepository.get().findById(contextId);
                    String namespace = null;
                    if (r != null) {
                        namespace = r.getNamespace();
                    } else if (resourceWithProfile instanceof Space) {
                        // The Space-creation templates' `space` placeholder has a fixed
                        // `https://w3id.org/spaces/` prefix, so the pre-fill is relative to it.
                        namespace = contextId.replaceFirst("https://w3id.org/spaces/", "") + "/";
                    }
                    if (namespace != null) {
                        params.set("param_" + partField, namespace + "<SET-SUFFIX>");
                    }
                }
                if (contextId != null) params.set("refresh-upon-publish", contextId);
                menuActions.add(new QueryResult.MenuAction(label, PublishPage.class, params));
            }
        }
        if (viewDisplay.getNanopubId() != null || !menuActions.isEmpty()) {
            add(new ViewDisplayMenu("np", viewDisplay, null, resourceWithProfile, menuActions));
        } else {
            add(new Label("np").setVisible(false));
        }
    }

}
