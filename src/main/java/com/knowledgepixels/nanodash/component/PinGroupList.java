package com.knowledgepixels.nanodash.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.template.Template;

public class PinGroupList extends Panel {

    public PinGroupList(String markupId, Space space) {
        super(markupId);

        final PageParameters params = new PageParameters();
        if (space.getDefaultProvenance() != null) {
            params.add("prtemplate", space.getDefaultProvenance().stringValue());
        }

        List<Pair<String, List<Serializable>>> pinnedResourcesList = new ArrayList<>();
        List<String> pinGroupTags = new ArrayList<>(space.getPinGroupTags());
        Collections.sort(pinGroupTags);
        List<Serializable> pinnedResources = new ArrayList<>(space.getPinnedResources());
        for (String tag : pinGroupTags) {
            for (Object pinned : space.getPinnedResourceMap().get(tag)) {
                if (pinnedResources.contains(pinned)) pinnedResources.remove(pinned);
            }
            pinnedResourcesList.add(Pair.of(tag, space.getPinnedResourceMap().get(tag)));
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
                            if (o instanceof Template t) return new TemplateItem("item", t, params);
                            if (o instanceof GrlcQuery q) return new QueryItem("item", q);
                            return null;
                        }));
            }
        });

        add(new WebMarkupContainer("emptynotice").setVisible(pinnedResourcesList.isEmpty()));
    }


}
