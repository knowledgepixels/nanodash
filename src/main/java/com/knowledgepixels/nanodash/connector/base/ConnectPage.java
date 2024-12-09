package com.knowledgepixels.nanodash.connector.base;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.nanopub.Nanopub;

import com.knowledgepixels.nanodash.NanodashPageRef;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.NanopubItem;
import com.knowledgepixels.nanodash.component.TitleBar;

import net.trustyuri.TrustyUriUtils;

public abstract class ConnectPage extends ConnectorPage {

	private static final long serialVersionUID = 1L;

	public ConnectPage(Nanopub np, PageParameters parameters) {
		this(np, parameters, true);
	}

	public ConnectPage(Nanopub np, PageParameters parameters, boolean doInit) {
		super(parameters);
		if (parameters == null) return;
		if (!doInit) return;
		init(np, parameters);
	}

	private void init(Nanopub np, PageParameters parameters) {
		add(new TitleBar("titlebar", this, "connectors",
				new NanodashPageRef(getConfig().getOverviewPage().getClass(), getConfig().getJournalName()),
				new NanodashPageRef(getConfig().getSelectPage().getClass(), "Create Nanopublication"),
				new NanodashPageRef("Connect")
			));
		add(new Image("logo", new PackageResourceReference(this.getClass(), getConfig().getLogoFileName())));

		add(new NanopubItem("nanopub", NanopubElement.get(np)));


		String uri = np.getUri().stringValue();
		String shortId = "np:" + Utils.getShortNanopubId(uri);
		String artifactCode = TrustyUriUtils.getArtifactCode(uri);
		String reviewUri = getConfig().getReviewUrlPrefix() + artifactCode;

		WebMarkupContainer inclusionPart = new WebMarkupContainer("includeinstruction");
		inclusionPart.add(new Image("form-submit", new PackageResourceReference(this.getClass(), getConfig().getSubmitImageFileName())));
		inclusionPart.add(new ExternalLink("np-link", reviewUri, reviewUri));
		inclusionPart.add(new ExternalLink("word-np-link", reviewUri, shortId));
		inclusionPart.add(new Label("latex-np-uri", reviewUri));
		inclusionPart.add(new Label("latex-np-label", shortId.replace("_", "\\_")));
		add(inclusionPart);
	}

}
