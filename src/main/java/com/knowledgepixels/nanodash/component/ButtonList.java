package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.IndividualAgent;
import com.knowledgepixels.nanodash.ResourceWithProfile;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.SpaceMemberRole;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;

import java.util.ArrayList;
import java.util.List;

public class ButtonList extends Panel {

    public ButtonList(String markupId, ResourceWithProfile resourceWithProfile, List<AbstractLink> buttons, List<AbstractLink> memberButtons, List<AbstractLink> adminButtons) {
        super(markupId);
        setOutputMarkupId(true);

        List<AbstractLink> allButtons = new ArrayList<>();
        if (buttons != null) {
            allButtons.addAll(buttons);
        }
        if (resourceWithProfile instanceof Space space) {
            if (SpaceMemberRole.isCurrentUserMember(space) && memberButtons != null) {
                allButtons.addAll(memberButtons);
            }
            if (SpaceMemberRole.isCurrentUserAdmin(space) && adminButtons != null) {
                allButtons.addAll(adminButtons);
            }
        } else if (resourceWithProfile instanceof IndividualAgent ia) {
            if (ia.isCurrentUser() && adminButtons != null) {
                allButtons.addAll(adminButtons);
            }
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
