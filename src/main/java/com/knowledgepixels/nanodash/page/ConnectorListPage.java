package com.knowledgepixels.nanodash.page;

import java.util.Arrays;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.knowledgepixels.nanodash.component.ProjectList;
import com.knowledgepixels.nanodash.component.TitleBar;
import com.knowledgepixels.nanodash.connector.ConnectorConfig;
import com.knowledgepixels.nanodash.connector.GenOverviewPage;

/**
 * A page that lists all available connectors.
 */
public class ConnectorListPage extends NanodashPage {

    private static final long serialVersionUID = 1L;

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/connectorlist";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    private static final String[] journals = new String[]{"ios/ds", "pensoft/bdj", "pensoft/rio"};

    /**
     * Returns the number of available connectors.
     *
     * @return the number of connectors
     */
    public static int getConnectorCount() {
        return journals.length;
    }

    /**
     * Constructor for the ConnectorListPage.
     *
     * @param parameters the page parameters
     */
    public ConnectorListPage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this, "connectors"));

        add(new DataView<String>("connectors", new ListDataProvider<String>(Arrays.asList(journals))) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<String> item) {
                String journalId = item.getModelObject();
                ConnectorConfig config = ConnectorConfig.get(journalId);
                BookmarkablePageLink<Void> l = new BookmarkablePageLink<>("connectorlink", GenOverviewPage.class, new PageParameters().add("journal", journalId));
                l.add(new Label("connectortext", config.getJournalName()));
                item.add(l);
            }

        });

        add(ProjectList.getListContainer("projects"));
    }

}
