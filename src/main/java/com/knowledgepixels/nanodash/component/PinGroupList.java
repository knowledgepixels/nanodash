package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.template.Template;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PinGroupList extends Panel {

    public PinGroupList(String markupId, Space space) {
        super(markupId);

        final PageParameters tParams = new PageParameters();
        tParams.set("param_space", space.getId());
        tParams.set("context", space.getId());
        if (space.getDefaultProvenance() != null) {
            tParams.set("prtemplate", space.getDefaultProvenance().stringValue());
        }

        final PageParameters qParams = new PageParameters();
        qParams.set("queryparam_space", space.getId());

        List<Pair<String, List<Serializable>>> pinnedResourcesList = new ArrayList<>();
        List<String> pinGroupTags = new ArrayList<>(space.getPinGroupTags());
        Collections.sort(pinGroupTags);
        List<Serializable> pinnedResources = new ArrayList<>(space.getPinnedResources());
        for (String tag : pinGroupTags) {
            for (Object pinned : space.getPinnedResourceMap().get(tag)) {
                pinnedResources.remove(pinned);
            }
            List<Serializable> list = new ArrayList<>(space.getPinnedResourceMap().get(tag));
            Collections.sort(list, new Comparator<Serializable>() {
                @Override
                public int compare(Serializable s0, Serializable s1) {
                    return getName(s0).compareTo(getName(s1));
                }
            });
            pinnedResourcesList.add(Pair.of(tag, list));
        }
        if (!pinnedResources.isEmpty()) {
            String l = pinnedResourcesList.isEmpty() ? "Resources" : "Other Resources";
            pinnedResourcesList.add(Pair.of(l, pinnedResources));
        }

        add(new DataView<Pair<String, List<Serializable>>>("pin-groups", new ListDataProvider<>(pinnedResourcesList)) {
            @Override
            protected void populateItem(Item<Pair<String, List<Serializable>>> item) {
                item.add(new ItemListPanel<Serializable>(
                        "pin-group",
                        item.getModelObject().getLeft(),
                        item.getModelObject().getRight(),
                        (o) -> {
                            if (o instanceof Template t) {
                                t.addToLabelMap(space.getId(), space.getLabel());
                                return new TemplateItem("item", t, tParams, false);
                            }
                            if (o instanceof GrlcQuery q) {
                                return new QueryItem("item", q, qParams, false);
                            }
                            return null;
                        }));
            }
        });

        add(new WebMarkupContainer("emptynotice").setVisible(pinnedResourcesList.isEmpty()));
    }

    private static String getName(Serializable s) {
        if (s instanceof GrlcQuery q) return q.getLabel();
        if (s instanceof Template t) return t.getLabel();
        return s.toString();
    }

}
