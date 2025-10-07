package com.knowledgepixels.nanodash.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;

import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.SpaceMemberRole;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.page.UserPage;

public class SpaceUserList extends Panel {

    public SpaceUserList(String markupId, Space space) {
        super(markupId);

        List<Pair<SpaceMemberRole, List<IRI>>> userLists = new ArrayList<>();
        for (SpaceMemberRole r : space.getRoles()) {
            List<IRI> userList = new ArrayList<>();
            for (IRI userId : space.getUsers()) {
                if (space.getMemberRoles(userId).contains(r)) userList.add(userId);
            }
            userLists.add(Pair.of(r, userList));
        }

        add(new DataView<Pair<SpaceMemberRole, List<IRI>>>("user-lists", new ListDataProvider<>(userLists)) {
            @Override
            protected void populateItem(Item<Pair<SpaceMemberRole, List<IRI>>> item) {
               item.add(new ItemListPanel<IRI>(
                       "user-list",
                       item.getModelObject().getLeft().getTitle(),
                       item.getModelObject().getRight(),
                       m -> {
                           return new ItemListElement("item", UserPage.class, new PageParameters().add("id", m), User.getShortDisplayName(m));
                       })
                   );
            }
        });
    }


}
