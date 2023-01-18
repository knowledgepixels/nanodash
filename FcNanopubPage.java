package org.petapico.nanobench.connector.ios;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.nanopub.Nanopub;
import org.petapico.nanobench.ApiAccess;
import org.petapico.nanobench.ApiResponse;
import org.petapico.nanobench.ApiResponseEntry;
import org.petapico.nanobench.ExplorePage;
import org.petapico.nanobench.NanobenchSession;
import org.petapico.nanobench.NanopubElement;
import org.petapico.nanobench.NanopubItem;
import org.petapico.nanobench.TitleBar;
import org.petapico.nanobench.Utils;

public class FcNanopubPage extends WebPage {

	private static final long serialVersionUID = 1L;

	public static final String MOUNT_PATH = "/connector-ios-fc-np";

	public FcNanopubPage(final PageParameters parameters) {
		add(new TitleBar("titlebar"));
		//add(new Label("titlebar"));  // hide title bar

		final NanobenchSession session = NanobenchSession.get();
		session.redirectToLoginIfNeeded(MOUNT_PATH, parameters);

		final String ref = parameters.get("id").toString();

		try {
			Nanopub np = Utils.getAsNanopub(ref);
			add(new NanopubItem("nanopub", new NanopubElement(np), false, true));
			String uri = np.getUri().stringValue();
			String shortId = "np:" + Utils.getShortNanopubId(uri);

			add(new Image("form-submit", new PackageResourceReference(this.getClass(), "FcFormSubmit.png")));

			add(new ExternalLink("np-link", uri, uri));
			add(new ExternalLink("word-np-link", uri, shortId));
			add(new Label("latex-np-uri", uri));
			add(new Label("latex-np-label", shortId.replace("_", "\\_")));

			Map<String,String> params = new HashMap<>();
			params.put("obj", uri);
			ApiResponse resp = ApiAccess.getAll(FcOverviewPage.apiUrl, "get-reactions", params);
	
			add(new DataView<ApiResponseEntry>("reactions", new ListDataProvider<ApiResponseEntry>(resp.getData())) {
	
				private static final long serialVersionUID = 1L;
	
				@Override
				protected void populateItem(Item<ApiResponseEntry> item) {
					ApiResponseEntry e = item.getModelObject();
					PageParameters params = new PageParameters();
					params.add("id", e.get("np"));
					BookmarkablePageLink<WebPage> l = new BookmarkablePageLink<WebPage>("reaction", ExplorePage.class, params);
					l.add(new Label("reactiontext", "\"" + e.get("text") + "\", " + e.get("person") + " " + e.get("date")));
					item.add(l);
				}
	
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
