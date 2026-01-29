package com.knowledgepixels.nanodash.component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class ViewList extends Panel {

    private static final Logger logger = LoggerFactory.getLogger(ViewList.class);

    public ViewList(String markupId, ProfiledResource profiledResource) {
        this(markupId, profiledResource, null, null, null);
    }

    public ViewList(String markupId, ProfiledResource profiledResource, String partId, String nanopubId, Set<IRI> partClasses) {
        super(markupId);

        final String id = (partId == null ? profiledResource.getId() : partId);
        final String npId = (nanopubId == null ? profiledResource.getNanopubId() : nanopubId);
        final List<ViewDisplay> viewDisplays;
        if (partId == null) {
            viewDisplays = profiledResource.getTopLevelViewDisplays();
        } else {
            viewDisplays = profiledResource.getPartLevelViewDisplays(partId, partClasses);
        }

        add(new DataView<ViewDisplay>("views", new ListDataProvider<ViewDisplay>(viewDisplays)) {

            @Override
            protected void populateItem(Item<ViewDisplay> item) {
                ResourceView view = item.getModelObject().getView();
                Multimap<String, String> queryRefParams = ArrayListMultimap.create();
                for (String p : view.getQuery().getPlaceholdersList()) {
                    String paramName = QueryParamField.getParamName(p);
                    if (paramName.equals(view.getQueryField())) {
                        queryRefParams.put(view.getQueryField(), id);
                        if (QueryParamField.isMultiPlaceholder(p) && profiledResource instanceof Space space) {
                            // TODO Support this also for maintained resources and users.
                            for (String altId : space.getAltIDs()) {
                                queryRefParams.put(view.getQueryField(), altId);
                            }
                        }
                    } else if (paramName.equals(view.getQueryField() + "Namespace") && profiledResource.getNamespace() != null) {
                        queryRefParams.put(view.getQueryField() + "Namespace", profiledResource.getNamespace());
                    } else if (paramName.equals(view.getQueryField() + "Np")) {
                        if (!QueryParamField.isOptional(p) && npId == null) {
                            queryRefParams.put(view.getQueryField() + "Np", "x:");
                        } else {
                            queryRefParams.put(view.getQueryField() + "Np", npId);
                        }
                    } else if (paramName.equals("user_pubkey") && QueryParamField.isMultiPlaceholder(p) && profiledResource instanceof Space space) {
                        for (IRI userId : space.getUsers()) {
                            for (String memberHash : User.getUserData().getPubkeyhashes(userId, true)) {
                                queryRefParams.put("user_pubkey", memberHash);
                            }
                        }
                    } else if (paramName.equals("admin_pubkey") && QueryParamField.isMultiPlaceholder(p) && profiledResource instanceof Space space) {
                        for (IRI adminId : space.getAdmins()) {
                            for (String adminHash : User.getUserData().getPubkeyhashes(adminId, true)) {
                                queryRefParams.put("admin_pubkey", adminHash);
                            }
                        }
                    } else if (!QueryParamField.isOptional(p)) {
                        item.add(new Label("view", "<span class=\"negative\">Error: Query has non-optional parameter</span>").setEscapeModelStrings(false));
                        logger.error("Error: Query has non-optional parameter: " + view.getQuery().getQueryId() + " " + p);
                        return;
                    }
                }
                QueryRef queryRef = new QueryRef(view.getQuery().getQueryId(), queryRefParams);
                if (view.getViewType() != null && ResourceView.getSupportedViewTypes().contains(view.getViewType())) {
                    if (view.getViewType().equals(KPXL_TERMS.LIST_VIEW)) {
                        item.add(QueryResultListBuilder.create("view", queryRef, item.getModelObject())
                                .space(profiledResource.getSpace())
                                .id(id)
                                .contextId(profiledResource.getId())
                                .build());
                    } else if (view.getViewType().equals(KPXL_TERMS.TABULAR_VIEW)) {
                        item.add(QueryResultTableBuilder.create("view", queryRef, item.getModelObject())
                                .profiledResource(profiledResource)
                                .contextId(profiledResource.getId())
                                .id(id)
                                .build());
                    } else if (view.getViewType().equals(KPXL_TERMS.PLAIN_PARAGRAPH_VIEW)) {
                        item.add(QueryResultPlainParagraphBuilder.create("view", queryRef, item.getModelObject())
                                .contextId(profiledResource.getId())
                                .id(id)
                                .build());
                    } else if (view.getViewType().equals(KPXL_TERMS.NANOPUB_SET_VIEW)) {
                        item.add(QueryResultNanopubSetBuilder.create("view", queryRef, item.getModelObject())
                                .contextId(profiledResource.getId())
                                .id(id)
                                .build());
                    } else {
                        item.add(new Label("view", "<span class=\"negative\">View type \"" + view.getViewType().stringValue() + "\" is supported but its view is not implemented yet</span>").setEscapeModelStrings(false));
                        logger.error("View type \"{}\" is supported but its view is not implemented yet", view.getViewType().stringValue());
                    }
                } else {
                    item.add(new Label("view", "<span class=\"negative\">Unsupported view type</span>").setEscapeModelStrings(false));
                    logger.error("Unsupported view type.");
                }
            }

        });

        add(new WebMarkupContainer("emptynotice").setVisible(viewDisplays.isEmpty()));
    }

}
