package com.knowledgepixels.nanodash.component;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;

import java.util.ArrayList;
import java.util.List;

public class BaseDisplayMenu extends Panel {

    public record Entry(String wicketId, Component component) {
    }

    private final List<Entry> entries = new ArrayList<>();

    public BaseDisplayMenu(String id) {
        super(id);
    }

    protected void addEntry(String wicketId, Component component) {
        entries.add(new Entry(wicketId, component));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        for (Entry entry : entries) {
            add(entry.component());
        }
    }

}
