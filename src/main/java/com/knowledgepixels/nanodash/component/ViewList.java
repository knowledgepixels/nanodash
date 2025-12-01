package com.knowledgepixels.nanodash.component;

import java.util.List;
import java.util.Set;

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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.MaintainedResource;
import com.knowledgepixels.nanodash.ProfiledResource;
import com.knowledgepixels.nanodash.ResourceView;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.ViewDisplay;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;

public class ViewList extends Panel {

    private static final Logger logger = LoggerFactory.getLogger(ViewList.class);

    public ViewList(String markupId, ProfiledResource profiledResource) {
        super(markupId);

        final List<ViewDisplay> topLevelViewDisplays = profiledResource.getViewDisplays(true, null);

        add(new DataView<ViewDisplay>("views", new ListDataProvider<ViewDisplay>(topLevelViewDisplays)) {

            @Override
            protected void populateItem(Item<ViewDisplay> item) {
                ResourceView view = item.getModelObject().getView();
                Multimap<String, String> queryRefParams = ArrayListMultimap.create();
                for (String p : view.getQuery().getPlaceholdersList()) {
                    String paramName = QueryParamField.getParamName(p);
                    if (paramName.equals(view.getQueryField())) {
                        queryRefParams.put(view.getQueryField(), profiledResource.getId());
                        if (QueryParamField.isMultiPlaceholder(p) && profiledResource instanceof Space space) {
                            // TODO Support this also for maintained resources and users.
                            for (String altId : space.getAltIDs()) {
                                queryRefParams.put(view.getQueryField(), altId);
                            }
                        }
                    } else if (paramName.equals(view.getQueryField() + "Np")) {
                        queryRefParams.put(view.getQueryField() + "Np", profiledResource.getNanopubId());
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
                if (view.getViewType().equals(KPXL_TERMS.TABULAR_VIEW)) {
                    item.add(QueryResultTableBuilder.create("view", queryRef, item.getModelObject())
                            .profiledResource(profiledResource)
                            .contextId(profiledResource.getId())
                            .id(profiledResource.getId())
                            .build());
                } else {
                    item.add(QueryResultListBuilder.create("view", queryRef, item.getModelObject())
                            .space(resource.getSpace())
                            .id(resource.getId())
                            .contextId(resource.getId())
                            .build());
                }
            }

        });

        add(new WebMarkupContainer("emptynotice").setVisible(topLevelViewDisplays.isEmpty()));
    }

    public ViewList(String markupId, ProfiledResource profiledResource, String partId, String nanopubId, Set<IRI> partClasses) {
        super(markupId);

        final String id;
        final List<ViewDisplay> viewDisplays;
        final String namespace;
        final Space space;

        if (profiledResource instanceof MaintainedResource r) {
            id = r.getId();
            viewDisplays = r.getViewDisplays(false, partClasses);
            namespace = r.getNamespace();
            space = r.getSpace();
        } else if (profiledResource instanceof Space s) {
            id = s.getId();
            viewDisplays = s.getViewDisplays(false, partClasses);
            namespace = null;
            space = s;
        } else {
            throw new IllegalArgumentException("Neither MaintainedResource nor Space: " + profiledResource);
        }

        add(new DataView<ViewDisplay>("views", new ListDataProvider<ViewDisplay>(viewDisplays)) {

            @Override
            protected void populateItem(Item<ViewDisplay> item) {
                ResourceView view = item.getModelObject().getView();
                Multimap<String, String> queryRefParams = ArrayListMultimap.create();
                for (String p : view.getQuery().getPlaceholdersList()) {
                    String paramName = QueryParamField.getParamName(p);
                    if (paramName.equals(view.getQueryField())) {
                        queryRefParams.put(view.getQueryField(), partId);
//                        if (QueryParamField.isMultiPlaceholder(p)) {
//                            for (String altId : resource.getAltIDs()) {
//                                queryRefParams.put(view.getQueryField(), altId);
//                            }
//                        }
                    } else if (paramName.equals(view.getQueryField() + "Namespace") && namespace != null) {
                        queryRefParams.put(view.getQueryField() + "Namespace", namespace);
                    } else if (paramName.equals(view.getQueryField() + "Np")) {
                        if (!QueryParamField.isOptional(p) && nanopubId == null) {
                            queryRefParams.put(view.getQueryField() + "Np", "x:");
                        } else {
                            queryRefParams.put(view.getQueryField() + "Np", nanopubId);
                        }
//                    } else if (paramName.equals("user_pubkey") && QueryParamField.isMultiPlaceholder(p)) {
//                        for (IRI userId : resource.getSpace().getUsers()) {
//                            for (String memberHash : User.getUserData().getPubkeyhashes(userId, true)) {
//                                queryRefParams.put("user_pubkey", memberHash);
//                            }
//                        }
                    } else if (!QueryParamField.isOptional(p)) {
                        item.add(new Label("view", "<span class=\"negative\">Error: Query has non-optional parameter.</span>").setEscapeModelStrings(false));
                        logger.error("Error: Query has non-optional parameter: " + view.getQuery().getQueryId() + " " + p);
                        return;
                    }
                }
                QueryRef queryRef = new QueryRef(view.getQuery().getQueryId(), queryRefParams);
                if (view.getViewType().equals(KPXL_TERMS.TABULAR_VIEW)) {
                    item.add(QueryResultTableBuilder.create("view", queryRef, item.getModelObject())
                            .profiledResource(profiledResource)
                            .id(partId)
                            .contextId(id)
                            .build());
                } else {
                    item.add(QueryResultListBuilder.create("view", queryRef, item.getModelObject())
                            .space(space)
                            .id(partId)
                            .contextId(id)
                            .build());
                }
            }

        });

        add(new WebMarkupContainer("emptynotice").setVisible(viewDisplays.isEmpty()));
    }


}
