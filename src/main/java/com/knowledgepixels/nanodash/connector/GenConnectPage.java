package com.knowledgepixels.nanodash.connector;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.NanopubItem;
import com.knowledgepixels.nanodash.component.TitleBar;
import net.trustyuri.TrustyUriUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.nanopub.Nanopub;

/**
 * Page for connecting a Nanopublication to a journal.
 */
public class GenConnectPage extends ConnectorPage {

    private static final long serialVersionUID = 1L;

    /**
     * Mount path for this page.
     */
    public static final String MOUNT_PATH = "/connector/connect";

    /**
     * Constructor for the GenConnectPage.
     *
     * @param np     the Nanopublication to connect
     * @param params the page parameters
     */
    public GenConnectPage(Nanopub np, PageParameters params) {
        super(params);
        add(new Label("pagetitle", getConfig().getJournalName() + ": Connect Nanopublication | nanodash"));

        PageParameters journalParam = new PageParameters().add("journal", getConnectorId());
        add(new TitleBar("titlebar", this, "connectors",
                new NanodashPageRef(GenOverviewPage.class, journalParam, getConfig().getJournalName()),
                new NanodashPageRef(GenSelectPage.class, journalParam, "Create Nanopublication"),
                new NanodashPageRef("Connect")
        ));
        add(new Image("logo", new PackageResourceReference(getConfig().getClass(), getConfig().getLogoFileName())));

        add(new NanopubItem("nanopub", NanopubElement.get(np)));


        String uri = np.getUri().stringValue();
        String shortId = "np:" + Utils.getShortNanopubId(uri);
        String artifactCode = TrustyUriUtils.getArtifactCode(uri);
        String reviewUri = getConfig().getReviewUrlPrefix() + artifactCode;

        WebMarkupContainer inclusionPart = new WebMarkupContainer("includeinstruction");
        inclusionPart.add(new Label("connectinstruction", getConfig().getConnectInstruction()).setEscapeModelStrings(false));
        inclusionPart.add(new Image("form-submit", new PackageResourceReference(getConfig().getClass(), getConfig().getSubmitImageFileName())));
        inclusionPart.add(new ExternalLink("np-link", reviewUri, reviewUri));
        inclusionPart.add(new ExternalLink("word-np-link", reviewUri, shortId));
        inclusionPart.add(new Label("latex-np-uri", reviewUri));
        inclusionPart.add(new Label("latex-np-label", shortId.replace("_", "\\_")));
        add(inclusionPart);

        add(new ExternalLink("support-link", "mailto:contact-project+knowledgepixels-support-desk@incoming.gitlab.com?subject=[" + getConfig().getJournalAbbrev() + "%20general]%20my%20problem/question&body=type%20your%20problem/question%20here"));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Get the mount path for this page.
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

}
