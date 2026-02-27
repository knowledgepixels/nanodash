package com.knowledgepixels.nanodash.component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.vocabulary.KPXL_TERMS;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.eclipse.rdf4j.model.IRI;
import org.nanopub.extra.services.QueryRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ViewList extends Panel {

    private static final Logger logger = LoggerFactory.getLogger(ViewList.class);

    public ViewList(String markupId, AbstractResourceWithProfile resourceWithProfile) {
        this(markupId, resourceWithProfile, null, null, null, null, null);
    }

    public ViewList(String markupId, AbstractResourceWithProfile resourceWithProfile, String partId, String nanopubId, Set<IRI> partClasses) {
        this(markupId, resourceWithProfile, partId, nanopubId, partClasses, null, null);
    }

    public ViewList(String markupId, AbstractResourceWithProfile resourceWithProfile, String partId, String nanopubId, Set<IRI> partClasses, AbstractResourceWithProfile footerResource, List<AbstractLink> footerAdminButtons) {
        super(markupId);

        final String id = (partId == null ? resourceWithProfile.getId() : partId);
        final String npId = (nanopubId == null ? resourceWithProfile.getNanopubId() : nanopubId);
        final List<ViewDisplay> viewDisplays;
        if (partId == null) {
            viewDisplays = resourceWithProfile.getTopLevelViewDisplays();
        } else {
            viewDisplays = resourceWithProfile.getPartLevelViewDisplays(partId, partClasses);
        }

        // Group viewDisplays by the first segment of their structural position (e.g. "4" from "4.4.1.papers")
        List<List<ViewDisplay>> groups = new ArrayList<>();
        String currentGroupKey = null;
        List<ViewDisplay> currentGroup = null;
        for (ViewDisplay vd : viewDisplays) {
            String pos = vd.getStructuralPosition();
            int firstDot = pos.indexOf('.');
            String key = firstDot > 0 ? pos.substring(0, firstDot) : pos;
            if (!key.equals(currentGroupKey)) {
                currentGroup = new ArrayList<>();
                groups.add(currentGroup);
                currentGroupKey = key;
            }
            currentGroup.add(vd);
        }

        add(new ListView<List<ViewDisplay>>("groups", groups) {
            @Override
            protected void populateItem(ListItem<List<ViewDisplay>> groupItem) {
                List<ViewDisplay> group = groupItem.getModelObject();
                groupItem.add(new ListView<ViewDisplay>("views", group) {
                    @Override
                    protected void populateItem(ListItem<ViewDisplay> item) {
                        View view = item.getModelObject().getView();
                        Multimap<String, String> queryRefParams = ArrayListMultimap.create();
                        for (String p : view.getQuery().getPlaceholdersList()) {
                            String paramName = QueryParamField.getParamName(p);
                            if (paramName.equals(view.getQueryField())) {
                                queryRefParams.put(view.getQueryField(), id);
                                if (QueryParamField.isMultiPlaceholder(p) && resourceWithProfile instanceof Space space) {
                                    // TODO Support this also for maintained resources and users.
                                    for (String altId : space.getAltIDs()) {
                                        queryRefParams.put(view.getQueryField(), altId);
                                    }
                                }
                            } else if (paramName.equals(view.getQueryField() + "Namespace") && resourceWithProfile.getNamespace() != null) {
                                queryRefParams.put(view.getQueryField() + "Namespace", resourceWithProfile.getNamespace());
                            } else if (paramName.equals(view.getQueryField() + "Np")) {
                                if (!QueryParamField.isOptional(p) && npId == null) {
                                    queryRefParams.put(view.getQueryField() + "Np", "x:");
                                } else {
                                    queryRefParams.put(view.getQueryField() + "Np", npId);
                                }
                            } else if (paramName.equals("user_pubkey") && QueryParamField.isMultiPlaceholder(p) && resourceWithProfile instanceof Space space) {
                                for (IRI userId : space.getUsers()) {
                                    for (String memberHash : User.getUserData().getPubkeyhashes(userId, true)) {
                                        queryRefParams.put("user_pubkey", memberHash);
                                    }
                                }
                            } else if (paramName.equals("admin_pubkey") && QueryParamField.isMultiPlaceholder(p) && resourceWithProfile instanceof Space space) {
                                for (IRI adminId : space.getAdmins()) {
                                    for (String adminHash : User.getUserData().getPubkeyhashes(adminId, true)) {
                                        queryRefParams.put("admin_pubkey", adminHash);
                                    }
                                }
                            } else if (!QueryParamField.isOptional(p)) {
                                item.add(new Label("view", "<span class=\"negative\">Error: Query has non-optional parameter</span>").setEscapeModelStrings(false));
                                logger.error("Error: Query has non-optional parameter: {} {}", view.getQuery().getQueryId(), p);
                                return;
                            }
                        }
                        QueryRef queryRef = new QueryRef(view.getQuery().getQueryId(), queryRefParams);
                        if (view.getViewType() != null && View.getSupportedViewTypes().contains(view.getViewType())) {
                            if (view.getViewType().equals(KPXL_TERMS.LIST_VIEW)) {
                                item.add(QueryResultListBuilder.create("view", queryRef, item.getModelObject())
                                        .space(resourceWithProfile.getSpace())
                                        .id(id)
                                        .contextId(resourceWithProfile.getId())
                                        .build());
                            } else if (view.getViewType().equals(KPXL_TERMS.TABULAR_VIEW)) {
                                item.add(QueryResultTableBuilder.create("view", queryRef, item.getModelObject())
                                        .profiledResource(resourceWithProfile)
                                        .contextId(resourceWithProfile.getId())
                                        .id(id)
                                        .build());
                            } else if (view.getViewType().equals(KPXL_TERMS.PLAIN_PARAGRAPH_VIEW)) {
                                item.add(QueryResultPlainParagraphBuilder.create("view", queryRef, item.getModelObject())
                                        .contextId(resourceWithProfile.getId())
                                        .id(id)
                                        .build());
                            } else if (view.getViewType().equals(KPXL_TERMS.NANOPUB_SET_VIEW)) {
                                item.add(QueryResultNanopubSetBuilder.create("view", queryRef, item.getModelObject())
                                        .contextId(resourceWithProfile.getId())
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
            }
        });

        add(new WebMarkupContainer("emptynotice").setVisible(viewDisplays.isEmpty()));

        WebMarkupContainer footerSection = new WebMarkupContainer("footer-section");
        if (footerAdminButtons != null) {
            footerSection.add(new ButtonList("footer-buttons",
                    footerResource != null ? footerResource : resourceWithProfile,
                    null, null, footerAdminButtons));
        } else {
            footerSection.setVisible(false);
            footerSection.add(new Label("footer-buttons").setVisible(false));
        }
        add(footerSection);
    }

}
