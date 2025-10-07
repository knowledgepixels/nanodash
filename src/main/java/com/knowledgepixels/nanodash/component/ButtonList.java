package com.knowledgepixels.nanodash.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.eclipse.rdf4j.model.IRI;

import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.SpaceMemberRole;

public class ButtonList extends Panel {

    public ButtonList(String markupId, Space space, List<AbstractLink> buttons, List<AbstractLink> memberButtons, List<AbstractLink> adminButtons) {
        super(markupId);
        setOutputMarkupId(true);

        List<AbstractLink> allButtons = new ArrayList<>();
        allButtons.addAll(buttons);
        if (isCurrentUserMember(space)) {
            allButtons.addAll(memberButtons);
        }
        if (isCurrentUserAdmin(space)) {
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

    private boolean isCurrentUserMember(Space space) {
        if (space == null) return false;
        IRI userIri = NanodashSession.get().getUserIri();
        if (userIri == null) return false;
        return space.isMember(userIri);
    }

    private boolean isCurrentUserAdmin(Space space) {
        if (space == null) return false;
        IRI userIri = NanodashSession.get().getUserIri();
        if (userIri == null) return false;
        if (space.getMemberRoles(userIri) == null) return false;
        return space.getMemberRoles(userIri).contains(SpaceMemberRole.ADMIN_ROLE);
    }

}
