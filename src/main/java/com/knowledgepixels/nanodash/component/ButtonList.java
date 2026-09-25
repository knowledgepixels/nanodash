package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.SpaceMemberRole;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.Space;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * The buttons shown for a resource, with the member-only and admin-only ones included according to
 * the roles the current user holds there.
 *
 * <p>The resource is held as a model and the roles are read when the list renders, so a page
 * restored from the page store shows the buttons the user may use now rather than the ones they
 * could use when the page was built (issue #459).
 */
public class ButtonList extends Panel {

    private final List<AbstractLink> buttons;
    private final List<AbstractLink> memberButtons;
    private final List<AbstractLink> adminButtons;

    /**
     * Constructor for ButtonList.
     *
     * @param markupId      the component id
     * @param resource      the resource the buttons act on
     * @param buttons       the buttons everyone sees
     * @param memberButtons the buttons only members of the resource's space see
     * @param adminButtons  the buttons only its admins see
     */
    public ButtonList(String markupId, IModel<? extends AbstractResourceWithProfile> resource, List<AbstractLink> buttons, List<AbstractLink> memberButtons, List<AbstractLink> adminButtons) {
        super(markupId, resource);
        setOutputMarkupId(true);
        this.buttons = buttons;
        this.memberButtons = memberButtons;
        this.adminButtons = adminButtons;

        add(new DataView<AbstractLink>("buttons", new ListDataProvider<AbstractLink>() {

            @Override
            protected List<AbstractLink> getData() {
                return visibleButtons();
            }

        }) {

            @Override
            protected void populateItem(Item<AbstractLink> item) {
                item.add(item.getModelObject());
            }

            @Override
            public boolean isVisible() {
                return !visibleButtons().isEmpty();
            }

        });
    }

    /**
     * @return the resource the buttons act on, as it is now
     */
    public AbstractResourceWithProfile getResource() {
        return (AbstractResourceWithProfile) getDefaultModelObject();
    }

    /**
     * Works out which buttons the current user may use on the resource as it is now.
     *
     * @return the buttons to show, in the order everyone-member-admin
     */
    private List<AbstractLink> visibleButtons() {
        List<AbstractLink> allButtons = new ArrayList<>();
        if (buttons != null) {
            allButtons.addAll(buttons);
        }
        AbstractResourceWithProfile resource = getResource();
        if (resource instanceof IndividualAgent individualAgent) {
            if (individualAgent.isCurrentUser() && adminButtons != null) {
                allButtons.addAll(adminButtons);
            }
            return allButtons;
        }
        Space space = resource instanceof Space s ? s : (resource == null ? null : resource.getSpace());
        if (space != null) {
            if (SpaceMemberRole.isCurrentUserMember(space) && memberButtons != null) {
                allButtons.addAll(memberButtons);
            }
            if (SpaceMemberRole.isCurrentUserAdmin(space) && adminButtons != null) {
                allButtons.addAll(adminButtons);
            }
        }
        return allButtons;
    }

}
