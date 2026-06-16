package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.domain.Space;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.SpacePage;
import com.knowledgepixels.nanodash.page.UserPage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;

import java.util.List;

/**
 * Lists all refs (root definitions) claiming a space's IRI — the explicit disambiguation
 * overview. Each distinct root definition is a separate space asserting the same
 * identifier; this shows each one's root nanopub and its admins so the viewer can tell
 * them apart. See docs/space-ref-identity.md.
 */
public class SpaceClaimantsPanel extends Panel {

    /**
     * @param markupId the Wicket markup id
     * @param space    the space whose IRI's claimants to list
     */
    public SpaceClaimantsPanel(String markupId, Space space) {
        super(markupId);
        final String spaceId = space.getId();
        List<Space.RefClaimant> claimants = space.getRefClaimants();
        add(new Label("iri", spaceId));
        add(new Label("count", Integer.toString(claimants.size())));
        add(new ListView<Space.RefClaimant>("claimants", claimants) {
            @Override
            protected void populateItem(ListItem<Space.RefClaimant> item) {
                Space.RefClaimant c = item.getModelObject();
                item.add(new BookmarkablePageLink<Void>("root-link", ExplorePage.class,
                        new PageParameters().add("id", c.getRootNp())).setBody(Model.of(shortNp(c.getRootNp()))));
                item.add(new WebMarkupContainer("current").setVisible(c.isRepresentative()));
                item.add(new BookmarkablePageLink<Void>("view-link", SpacePage.class,
                        new PageParameters().add("id", spaceId).add("root", c.getRootNp())));
                item.add(new ListView<String>("admins", c.getAdmins()) {
                    @Override
                    protected void populateItem(ListItem<String> adminItem) {
                        String agent = adminItem.getModelObject();
                        IRI agentIri = Utils.vf.createIRI(agent);
                        adminItem.add(new Label("admin-sep", ", ").setVisible(adminItem.getIndex() > 0));
                        adminItem.add(new BookmarkablePageLink<Void>("admin-link", UserPage.class,
                                new PageParameters().add("id", agent)).setBody(Model.of(User.getShortDisplayName(agentIri))));
                    }
                });
                item.add(new WebMarkupContainer("no-admins").setVisible(c.getAdmins().isEmpty()));
            }
        });
    }

    /** A short, human-scannable form of a nanopub IRI (its artifact-code prefix). */
    private static String shortNp(String npIri) {
        int i = Math.max(npIri.lastIndexOf('/'), npIri.lastIndexOf('#'));
        String code = i < 0 ? npIri : npIri.substring(i + 1);
        return code.length() > 16 ? code.substring(0, 16) + "…" : code;
    }

}
