package com.knowledgepixels.nanodash.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.page.NanodashPage;

/**
 * TitleBar is the top bar of the Nanodash application, which contains
 * navigation elements such as profile, my channel, users, connectors,
 * publish, query, and breadcrumb navigation.
 */
public class TitleBar extends Panel {

    private static final long serialVersionUID = 1L;

    private String highlight;

    /**
     * Constructs a TitleBar with the specified id, page, highlight element,
     * and an array of path references for breadcrumb navigation.
     *
     * @param id        the component id
     * @param page      the current Nanodash page
     * @param highlight the id of the element to highlight
     * @param pathRefs  an array of NanodashPageRef for breadcrumb navigation
     */
    public TitleBar(String id, NanodashPage page, String highlight, NanodashPageRef... pathRefs) {
        super(id);
        this.highlight = highlight;
        add(new ProfileItem("profile", page));

        createContainer("mychannel").setVisible(!NanodashPreferences.get().isReadOnlyMode());
        createContainer("users");
        createContainer("connectors");
        createContainer("publish").setVisible(!NanodashPreferences.get().isReadOnlyMode());
        createContainer("query");

        WebMarkupContainer breadcrumbPath = new WebMarkupContainer("breadcrumbpath");
        breadcrumbPath.setVisible(pathRefs.length > 0);
        if (pathRefs.length > 0) {
            breadcrumbPath.add(pathRefs[0].createComponent("firstpathelement"));
            // Getting serialization exception if not using 'new ArrayList<...>(...)' here:
            List<NanodashPageRef> morePathElements = new ArrayList<NanodashPageRef>(Utils.subList(pathRefs, 1, pathRefs.length));
            breadcrumbPath.add(new DataView<NanodashPageRef>("morepathelements", new ListDataProvider<NanodashPageRef>(morePathElements)) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(Item<NanodashPageRef> item) {
                    item.add(item.getModelObject().createComponent("furtherpathelement"));
                }

            });
        } else {
            breadcrumbPath.setVisible(false);
        }
        add(breadcrumbPath);
    }

    private WebMarkupContainer createContainer(String id) {
        WebMarkupContainer c = new WebMarkupContainer(id);
        if (id.equals(highlight)) {
            c.add(new AttributeAppender("class", "selected"));
        }
        add(c);
        return c;
    }

}
