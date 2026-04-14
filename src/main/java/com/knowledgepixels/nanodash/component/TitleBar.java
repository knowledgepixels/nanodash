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

        createContainer("users");
        createContainer("connectors");
        createContainer("publish").setVisible(!NanodashPreferences.get().isReadOnlyMode());
        createContainer("query");

        WebMarkupContainer breadcrumbPath = new WebMarkupContainer("breadcrumbpath");
        breadcrumbPath.setVisible(pathRefs.length > 0);
        if (pathRefs.length > 0) {
            final String[] displayLabels = simplifyBreadcrumbLabels(pathRefs);
            breadcrumbPath.add(pathRefs[0].createComponent("firstpathelement", displayLabels[0]));
            // Getting serialization exception if not using 'new ArrayList<...>(...)' here:
            List<NanodashPageRef> morePathElements = new ArrayList<NanodashPageRef>(Utils.subList(pathRefs, 1, pathRefs.length));
            breadcrumbPath.add(new DataView<NanodashPageRef>("morepathelements", new ListDataProvider<NanodashPageRef>(morePathElements)) {

                @Override
                protected void populateItem(Item<NanodashPageRef> item) {
                    int index = (int) item.getIndex() + 1;
                    item.add(item.getModelObject().createComponent("furtherpathelement", displayLabels[index]));
                }

            });
        } else {
            breadcrumbPath.setVisible(false);
        }
        add(breadcrumbPath);
    }

    /**
     * Computes shortened display labels for a breadcrumb path.
     *
     * Two simplifications are applied:
     * <ul>
     *   <li>If a non-root crumb's label starts with the previous crumb's label
     *       followed by a space (e.g. parent "Knowledge Pixels", child
     *       "Knowledge Pixels Incubator"), the shared prefix is stripped so
     *       only "Incubator" is shown.</li>
     *   <li>Any ": " in a label and everything after it is removed (e.g.
     *       "Incubator 1: Some title" becomes "Incubator 1"). Applied to all
     *       crumbs, including the first.</li>
     * </ul>
     *
     * The parent-prefix comparison uses the original (un-simplified) labels,
     * so that simplifications on the parent don't interfere with the child.
     */
    static String[] simplifyBreadcrumbLabels(NanodashPageRef[] pathRefs) {
        String[] displayLabels = new String[pathRefs.length];
        for (int i = 0; i < pathRefs.length; i++) {
            String label = pathRefs[i].getLabel();
            if (label != null) {
                if (i > 0) {
                    String parent = pathRefs[i - 1].getLabel();
                    if (parent != null && !parent.isEmpty() && label.startsWith(parent + " ")) {
                        label = label.substring(parent.length() + 1);
                    }
                }
                int colonIdx = label.indexOf(": ");
                if (colonIdx >= 0) {
                    label = label.substring(0, colonIdx);
                }
            }
            displayLabels[i] = label;
        }
        return displayLabels;
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
