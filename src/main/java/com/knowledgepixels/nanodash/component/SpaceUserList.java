package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.SpaceMemberRole;
import com.knowledgepixels.nanodash.SpaceMemberRoleRef;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.page.UserPage;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;

import java.util.ArrayList;
import java.util.List;

/**
 * The members of a space, one list per role.
 *
 * <p>The space is held as a model and its roles and members are read when the list renders, so a
 * page restored from the page store shows who holds which role now rather than who held it when
 * the page was built (issue #459).
 */
public class SpaceUserList extends Panel {

    /**
     * Constructor for SpaceUserList.
     *
     * @param markupId the component id
     * @param space    the space whose members to list
     */
    public SpaceUserList(String markupId, IModel<Space> space) {
        super(markupId, space);

        add(new DataView<Pair<SpaceMemberRole, List<Pair<IRI, String>>>>("user-lists", new ListDataProvider<Pair<SpaceMemberRole, List<Pair<IRI, String>>>>() {

            @Override
            protected List<Pair<SpaceMemberRole, List<Pair<IRI, String>>>> getData() {
                return membersPerRole();
            }

        }) {

            @Override
            protected void populateItem(Item<Pair<SpaceMemberRole, List<Pair<IRI, String>>>> item) {
                item.add(userListPanel(item.getModelObject().getLeft(), item.getModelObject().getRight()));
            }

        });
    }

    /**
     * @return the space whose members this list shows, as it is now
     */
    public Space getSpace() {
        return (Space) getDefaultModelObject();
    }

    /**
     * Collects, for each role of the space, the members holding it together with the
     * nanopublication that assigned it.
     *
     * @return one entry per role, each carrying pairs of user id and nanopublication id
     */
    private List<Pair<SpaceMemberRole, List<Pair<IRI, String>>>> membersPerRole() {
        Space space = getSpace();
        List<Pair<SpaceMemberRole, List<Pair<IRI, String>>>> userLists = new ArrayList<>();
        if (space == null) return userLists;
        for (SpaceMemberRoleRef r : space.getRoles()) {
            List<Pair<IRI, String>> userList = new ArrayList<>();
            for (IRI userId : space.getUsers()) {
                for (SpaceMemberRoleRef p : space.getMemberRoles(userId)) {
                    if (p.getRole().equals(r.getRole())) {
                        userList.add(Pair.of(userId, p.getNanopubUri()));
                        break;
                    }
                }
            }
            userLists.add(Pair.of(r.getRole(), userList));
        }
        return userLists;
    }

    @SuppressWarnings("unchecked")
    private ItemListPanel<Pair<IRI, String>> userListPanel(SpaceMemberRole role, List<Pair<IRI, String>> members) {
        Space space = getSpace();
        ItemListPanel<Pair<IRI, String>> panel = new ItemListPanel<>(
                "user-list",
                role.getTitle(),
                members,
                // FIXME add the source nanopublication
                m -> new ItemListElement("item", UserPage.class, new PageParameters().add("id", m.getLeft()), User.getShortDisplayName(m.getLeft()), null, Utils.getAsNanopub(m.getRight())))
                .setResourceWithProfile((IModel<Space>) getDefaultModel());
        if (role.getRoleAssignmentTemplate() != null) {
            if (!role.isAdminRole() || SpaceMemberRole.isCurrentUserAdmin(space)) {
                panel.addButton("+", PublishPage.class, new PageParameters()
                        .set("template", role.getRoleAssignmentTemplate().getId())
                        .set("param_space", space.getId())
                        .set("refresh-upon-publish", space.getId())
                        .set("template-version", "latest")
                );
            }
        }
        return panel;
    }

}
