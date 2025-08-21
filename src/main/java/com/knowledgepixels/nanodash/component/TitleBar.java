package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.NanodashPreferences;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.page.ConnectorListPage;
import com.knowledgepixels.nanodash.page.NanodashPage;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;

import java.util.ArrayList;
import java.util.List;

public class TitleBar extends Panel {

    private static final long serialVersionUID = 1L;

    private String highlight;

    public TitleBar(String id, NanodashPage page, String highlight, NanodashPageRef... pathRefs) {
        super(id);
        this.highlight = highlight;
        add(new ProfileItem("profile", page));

        createContainer("mychannel").setVisible(!NanodashPreferences.get().isReadOnlyMode());
        createContainer("users");
        createContainer("connectors").setVisible(ConnectorListPage.getConnectorCount() > 0);
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
