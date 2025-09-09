package com.knowledgepixels.nanodash.page;

import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.*;
import jakarta.servlet.http.HttpServletRequest;
import net.trustyuri.TrustyUriUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.commonjava.mimeparse.MIMEParse;
import org.nanopub.Nanopub;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.vocabulary.NTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * ExplorePage is a page that allows users to explore a specific Nanopublication or Thing.
 */
public class ExplorePage extends NanodashPage {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ExplorePage.class);

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/explore";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMountPath() {
        return MOUNT_PATH;
    }

    /**
     * Constructor for ExplorePage.
     *
     * @param parameters Page parameters containing the ID of the Nanopublication or Thing to explore.
     */
    public ExplorePage(final PageParameters parameters) {
        super(parameters);

        add(new TitleBar("titlebar", this, null));

        String tempRef = parameters.get("id").toString();

        WebMarkupContainer raw = new WebMarkupContainer("raw");
        add(raw);

        Map<String, String> nanopubParams = new HashMap<>();
        nanopubParams.put("ref", tempRef);
        Nanopub np = Utils.getAsNanopub(tempRef);
        boolean isNanopubId = (np != null);
        if (isNanopubId) {
            tempRef = np.getUri().stringValue();
        }
        if (!isNanopubId && tempRef.matches("^(.*[^A-Za-z0-9-_])?RA[A-Za-z0-9-_]{43}[^A-Za-z0-9-_].*$")) {
            np = Utils.getAsNanopub(tempRef.replaceFirst("^(.*[^A-Za-z0-9-_])?(RA[A-Za-z0-9-_]{43})[^A-Za-z0-9-_].*$", "$2"));
            if (np != null) {
                String npId = np.getUri().stringValue();
                tempRef = npId + tempRef.replaceFirst("^(.*[^A-Za-z0-9-_])?(RA[A-Za-z0-9-_]{43})([^A-Za-z0-9-_].*)$", "$3");
                Map<String, String> params = new HashMap<>();
                params.put("thing", tempRef);
                params.put("np", npId);
                ApiResponse resp = QueryApiAccess.forcedGet("get-latest-thing-nanopub", params);
                if (!resp.getData().isEmpty()) {
                    // TODO We take the most recent in case more than one latest version exists. Make other latest versions visible too.
                    npId = resp.getData().get(0).get("latestVersion");
                }
                np = Utils.getAsNanopub(npId);
            }
        }
        if (np == null) {
            raw.setVisible(false);
            add(new Label("nanopub-header", ""));
            add(new Label("nanopub", ""));
            add(new WebMarkupContainer("use-template").setVisible(false));
            add(new WebMarkupContainer("run-query").setVisible(false));
        } else {

            // Check whether we should redirect to Nanopub Registry for machine-friendly formats:
            String mimeType = Utils.TYPE_HTML;
            try {
                HttpServletRequest httpRequest = (HttpServletRequest) getRequest().getContainerRequest();
                mimeType = MIMEParse.bestMatch(Utils.SUPPORTED_TYPES_LIST, httpRequest.getHeader("Accept"));
            } catch (Exception ex) {
                logger.error("Error determining MIME type from Accept header.", ex);
            }
            if (!mimeType.equals(Utils.TYPE_HTML)) {
                logger.info("Non-HTML content type: {}", mimeType);
                // TODO Make this registry URL configurable/dynamic:
                String redirectUrl = Utils.getMainRegistryUrl() + "np/" + TrustyUriUtils.getArtifactCode(np.getUri().stringValue());
                logger.info("Redirecting to: {}", redirectUrl);
                throw new RedirectToUrlException(redirectUrl, 302);
            }

            String nanopubHeaderLabel = "<h4>%s</h4>";
            if (isNanopubId) {
                nanopubHeaderLabel = String.format(nanopubHeaderLabel, "Nanopublication");
            } else {
                nanopubHeaderLabel = String.format(nanopubHeaderLabel, "Minted in Nanopublication");
            }
            add(new Label("nanopub-header", nanopubHeaderLabel).setEscapeModelStrings(false));
            add(new NanopubItem("nanopub", NanopubElement.get(np)));
            String url = "http://np.knowledgepixels.com/" + TrustyUriUtils.getArtifactCode(np.getUri().stringValue());
            raw.add(new ExternalLink("trig-txt", url + ".trig.txt"));
            raw.add(new ExternalLink("jsonld-txt", url + ".jsonld.txt"));
            raw.add(new ExternalLink("nq-txt", url + ".nq.txt"));
            raw.add(new ExternalLink("xml-txt", url + ".xml.txt"));
            raw.add(new ExternalLink("trig", url + ".trig"));
            raw.add(new ExternalLink("jsonld", url + ".jsonld"));
            raw.add(new ExternalLink("nq", url + ".nq"));
            raw.add(new ExternalLink("xml", url + ".xml"));
            if (Utils.isNanopubOfClass(np, NTEMPLATE.ASSERTION_TEMPLATE)) {
                add(new WebMarkupContainer("use-template").add(new BookmarkablePageLink<Void>("template-link", PublishPage.class, new PageParameters().add("template", np.getUri()))));
            } else {
                add(new WebMarkupContainer("use-template").setVisible(false));
            }
            if (Utils.isNanopubOfClass(np, GrlcQuery.GRLC_QUERY_CLASS)) {
                add(new WebMarkupContainer("run-query").add(new BookmarkablePageLink<Void>("query-link", QueryPage.class, new PageParameters().add("id", np.getUri()))));
            } else {
                add(new WebMarkupContainer("run-query").setVisible(false));
            }
        }

        final String ref = tempRef;
        final String shortName;
        if (parameters.get("label").isEmpty()) {
            shortName = IriItem.getShortNameFromURI(ref);
        } else {
            shortName = parameters.get("label").toString();
        }
        add(new Label("pagetitle", shortName + " (explore) | nanodash"));
        add(new Label("termname", shortName));
        add(new ExternalLink("urilink", ref, ref));
        if (isNanopubId && SignatureUtils.seemsToHaveSignature(np)) {
            add(StatusLine.createComponent("statusline", ref));
        } else {
            add(new Label("statusline").setVisible(false));
        }
        add(ThingListPanel.createComponent("classes-panel", ThingListPanel.Mode.CLASSES, ref, "<em>Searching for classes...</em>"));
        if (isNanopubId) {
            add(new Label("instances-panel").setVisible(false));
            add(new Label("parts-panel").setVisible(false));
            add(new Label("templates-panel").setVisible(false));
        } else {
            add(ThingListPanel.createComponent("instances-panel", ThingListPanel.Mode.INSTANCES, ref, "<em>Searching for instances...</em>"));
            add(ThingListPanel.createComponent("parts-panel", ThingListPanel.Mode.PARTS, ref, "<em>Searching for parts...</em>"));
            add(ThingListPanel.createComponent("templates-panel", ThingListPanel.Mode.TEMPLATES, ref, "<em>Searching for templates...</em>"));
        }
        add(ExploreDataTable.createComponent("reftable", ref));
    }

}
