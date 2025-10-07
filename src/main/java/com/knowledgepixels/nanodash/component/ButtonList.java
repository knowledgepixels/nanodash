package com.knowledgepixels.nanodash.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;

import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.SpaceMemberRole;

public class ButtonList extends Panel {

    public ButtonList(String markupId, Space space, List<AbstractLink> buttons, List<AbstractLink> memberButtons, List<AbstractLink> adminButtons) {
        super(markupId);
        setOutputMarkupId(true);

        List<AbstractLink> allButtons = new ArrayList<>();
        allButtons.addAll(buttons);
        if (SpaceMemberRole.isCurrentUserMember(space)) {
            allButtons.addAll(memberButtons);
        }
        if (SpaceMemberRole.isCurrentUserAdmin(space)) {
            allButtons.addAll(adminButtons);
        }
        if (allButtons.isEmpty()) {
            add(new Label("buttons").setVisible(false));
        } else {
            add(new DataView<AbstractLink>("buttons", new ListDataProvider<AbstractLink>(allButtons)) {

                @Override
                protected void populateItem(Item<AbstractLink> item) {
                    item.add(item.getModelObject());
                }
                
            });
        }
    }

}
