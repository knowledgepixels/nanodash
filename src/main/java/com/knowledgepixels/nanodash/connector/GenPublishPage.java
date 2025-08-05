package com.knowledgepixels.nanodash.connector;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.component.PublishForm;
import com.knowledgepixels.nanodash.component.TitleBar;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.Random;

/**
 * Page for publishing a nanopublication.
 */
public class GenPublishPage extends ConnectorPage {

    private static final long serialVersionUID = 1L;

    /**
     * Mount path for this page.
     */
    public static final String MOUNT_PATH = "/connector/publish";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for the GenPublishPage.
     *
     * @param parameters Page parameters containing the necessary information to render the page.
     */
    public GenPublishPage(final PageParameters parameters) {
        super(parameters);
        add(new Label("pagetitle", getConfig().getJournalName() + ": Publish Nanopublication | nanodash"));

        final NanodashSession session = NanodashSession.get();
        PageParameters journalParam = new PageParameters().add("journal", getConnectorId());
        add(new TitleBar("titlebar", this, "connectors",
                new NanodashPageRef(GenOverviewPage.class, journalParam, getConfig().getJournalName()),
                new NanodashPageRef(GenSelectPage.class, journalParam, "Create Nanopublication"),
                new NanodashPageRef("Publish")
        ));
        add(new Image("logo", new PackageResourceReference(getConfig().getClass(), getConfig().getLogoFileName())));

        if (parameters.get("template").toString() != null) {
            if (!parameters.contains("formobj")) {
                throw new RestartResponseException(getClass(), parameters.add("formobj", Math.abs(new Random().nextLong()) + ""));
            }
            parameters.add("template-version", "latest");
            String formObjId = parameters.get("formobj").toString();
            if (!session.hasForm(formObjId)) {
                PublishForm publishForm = new PublishForm("form", parameters, getClass(), GenConnectPage.class);
                session.setForm(formObjId, publishForm);
            }
            add(session.getForm(formObjId));
        } else {
            throw new RuntimeException("no template parameter");
        }

        ConnectorOption option = ConnectorOption.valueOf(parameters.get("type").toString().toUpperCase());
        PageParameters pageParams = new PageParameters().add("journal", getConnectorId()).add("id", option.getExampleId()).add("mode", "final");
        add(new BookmarkablePageLink<Void>("show-example", GenNanopubPage.class, pageParams));
        add(new ExternalLink("support-link", "mailto:contact-project+knowledgepixels-support-desk@incoming.gitlab.com?subject=[" + getConfig().getJournalAbbrev() + "%20general]%20my%20problem/question&body=type%20your%20problem/question%20here"));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Renders the head section of the page, including custom JavaScript functions.
     */
    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        // TODO: There is probably a better place to define this function:
        response.render(JavaScriptHeaderItem.forScript(
                "function disableTooltips() { $('.select2-selection__rendered').prop('title', ''); }\n" +
                        //"$(document).ready(function() { $('.select2-static').select2(); });",  // for static select2 textfields
                        "",
                "custom-functions"));
    }

}
