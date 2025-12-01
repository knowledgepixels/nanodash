package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.page.PublishPage;
import com.knowledgepixels.nanodash.page.UserPage;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;

import java.util.ArrayList;
import java.util.List;

public class SpaceUserList extends Panel {

    public SpaceUserList(String markupId, Space space) {
        super(markupId);

        List<Pair<SpaceMemberRole, List<Pair<IRI, String>>>> userLists = new ArrayList<>();
        for (SpaceMemberRoleRef r : space.getRoles()) {
            // list of pairs of userId + nanopubId:
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

        add(new DataView<Pair<SpaceMemberRole, List<Pair<IRI, String>>>>("user-lists", new ListDataProvider<>(userLists)) {
            @Override
            protected void populateItem(Item<Pair<SpaceMemberRole, List<Pair<IRI, String>>>> item) {
                SpaceMemberRole role = item.getModelObject().getLeft();
                ItemListPanel<Pair<IRI, String>> panel = new ItemListPanel<>(
                        "user-list",
                        role.getTitle(),
                        item.getModelObject().getRight(),
                        // FIXME add the source nanopublication
                        m -> new ItemListElement("item", UserPage.class, new PageParameters().add("id", m.getLeft()), User.getShortDisplayName(m.getLeft()), null, Utils.getAsNanopub(m.getRight()))).setProfiledResource(space);
                if (role.getRoleAssignmentTemplate() != null) {
                    if (!role.isAdminRole() || SpaceMemberRole.isCurrentUserAdmin(space)) {
                        panel.addButton("+", PublishPage.class, new PageParameters()
                                .set("template", role.getRoleAssignmentTemplate().getId())
                                .set("param_space", space.getId())
                                .set("refresh-upon-publish", space.getId())
                        );
                    }
                }
                item.add(panel);
            }
        });
    }

}
