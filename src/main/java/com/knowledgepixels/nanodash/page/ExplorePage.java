package com.knowledgepixels.nanodash.page;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.INamedParameters.NamedPair;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.commonjava.mimeparse.MIMEParse;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.NTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.GrlcQuery;
import com.knowledgepixels.nanodash.MaintainedResource;
import com.knowledgepixels.nanodash.NanodashSession;
import com.knowledgepixels.nanodash.NanopubElement;
import com.knowledgepixels.nanodash.QueryApiAccess;
import com.knowledgepixels.nanodash.Space;
import com.knowledgepixels.nanodash.User;
import com.knowledgepixels.nanodash.Utils;
import com.knowledgepixels.nanodash.component.ExploreDataTable;
import com.knowledgepixels.nanodash.component.IriItem;
import com.knowledgepixels.nanodash.component.NanopubItem;
import com.knowledgepixels.nanodash.component.StatusLine;
import com.knowledgepixels.nanodash.component.ThingListPanel;
import com.knowledgepixels.nanodash.component.TitleBar;

import jakarta.servlet.http.HttpServletRequest;
import net.trustyuri.TrustyUriUtils;

/**
 * ExplorePage is a page that allows users to explore a specific Nanopublication or Thing.
 */
public class ExplorePage extends NanodashPage {

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

    private Nanopub publishedNanopub = null;

    /**
     * Constructor for ExplorePage.
     *
     * @param parameters Page parameters containing the ID of the Nanopublication or Thing to explore.
     */
    public ExplorePage(final PageParameters parameters) {
        super(parameters);
        add(new Label("publish-confirm-panel").setVisible(false));
        initPage();
    }

    public ExplorePage(final Nanopub publishedNanopub, final PageParameters parameters) {
        super(parameters.add("id", publishedNanopub.getUri()));
        this.publishedNanopub = publishedNanopub;

        WebMarkupContainer publishConfirmPanel = new WebMarkupContainer("publish-confirm-panel");
        final NanodashSession session = NanodashSession.get();
        boolean hasKnownOwnLocalIntro = session.getLocalIntroCount() > 0;
        boolean someIntroJustNowPublished = Utils.usesPredicateInAssertion(publishedNanopub, NPX.DECLARED_BY);
        if (someIntroJustNowPublished) NanodashSession.get().setIntroPublishedNow();
        boolean lastIntroPublishedMoreThanFiveMinsAgo = session.getTimeSinceLastIntroPublished() > 5 * 60 * 1000;
        if (!hasKnownOwnLocalIntro && session.hasIntroPublished()) User.refreshUsers();
        publishConfirmPanel.add(new WebMarkupContainer("missing-intro-warning").setVisible(!hasKnownOwnLocalIntro && lastIntroPublishedMoreThanFiveMinsAgo));

        PageParameters plainLinkParams = new PageParameters();
        plainLinkParams.add("template", parameters.get("template"));
        if (!parameters.get("template-version").isEmpty()) {
            plainLinkParams.add("template-version", parameters.get("template-version"));
        }
        publishConfirmPanel.add(new BookmarkablePageLink<Void>("publish-another-link", PublishPage.class, plainLinkParams));

        PageParameters linkParams = new PageParameters(parameters);
        linkParams.remove("supersede");
        linkParams.remove("supersede-a");
        boolean publishAnotherFilledLinkVisible = false;
        for (NamedPair n : linkParams.getAllNamed()) {
            if (n.getKey().matches("(param|prparam|piparam[1-9][0-9]*)_.*")) {
                publishAnotherFilledLinkVisible = true;
                break;
            }
        }
        if (publishAnotherFilledLinkVisible) {
            publishConfirmPanel.add(new BookmarkablePageLink<Void>("publish-another-filled-link", PublishPage.class, linkParams));
        } else {
            publishConfirmPanel.add(new Label("publish-another-filled-link", "").setVisible(false));
        }
        add(publishConfirmPanel);

        initPage();
    }

    private void initPage() {
        PageParameters parameters = getPageParameters();

        add(new TitleBar("titlebar", this, null));

        String tempRef = parameters.get("id").toString();

        String contextId = parameters.get("context").toString("");
        if (Space.get(contextId) != null) {
            add(new BookmarkablePageLink<Void>("back-to-context-link", SpacePage.class, new PageParameters().add("id", contextId)).setBody(Model.of("back to " + Space.get(contextId).getLabel())));
        } else if (MaintainedResource.get(contextId) != null) {
            add(new BookmarkablePageLink<Void>("back-to-context-link", MaintainedResourcePage.class, new PageParameters().add("id", contextId)).setBody(Model.of("back to " + MaintainedResource.get(contextId).getLabel())));
        } else {
            add(new Label("back-to-context-link").setVisible(false));
        }

        if (User.getUserData().isUser(tempRef)) {
            add(new BookmarkablePageLink<Void>("to-specific-page-link", UserPage.class, parameters).setBody(Model.of("go to user page")));
        } else if (Space.get(tempRef) != null) {
            add(new BookmarkablePageLink<Void>("to-specific-page-link", SpacePage.class, parameters).setBody(Model.of("go to Space page")));
        } else {
            add(new Label("to-specific-page-link").setVisible(false));
        }

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
                Multimap<String, String> params = ArrayListMultimap.create();
                params.put("thing", tempRef);
                params.put("np", npId);
                ApiResponse resp = QueryApiAccess.forcedGet(new QueryRef("get-latest-thing-nanopub", params));
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

            if (parameters.get("forward-to-part").toString("").equals("true")) {
                parameters.remove("forward-to-part");
                Set<String> introducedIds = Utils.getIntroducedIriIds(np);
                if (introducedIds.size() == 1) {
                    String subj = introducedIds.iterator().next();
                    for (Statement st : np.getAssertion()) {
                        if (!st.getSubject().stringValue().equals(subj)) continue;
                        if (!st.getPredicate().equals(DCTERMS.IS_PART_OF) && !st.getPredicate().equals(SKOS.IN_SCHEME)) continue;
                        String resourceId = st.getObject().stringValue();
                        if (MaintainedResource.get(resourceId) == null) continue;
                        throw new RestartResponseException(ResourcePartPage.class, parameters);
                    }
                }
            }

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
            String url = Utils.getMainRegistryUrl() + "np/" + TrustyUriUtils.getArtifactCode(np.getUri().stringValue());
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
        if (publishedNanopub != null) {
            shortName = NanopubUtils.getLabel(np);
        } else if (parameters.get("label").isEmpty()) {
            shortName = IriItem.getShortNameFromURI(ref);
        } else {
            shortName = parameters.get("label").toString();
        }
        add(new Label("pagetitle", shortName + " (explore) | nanodash"));
        add(new Label("termname", shortName));
        add(new ExternalLink("urilink", ref, ref));
        if (publishedNanopub != null) {
            add(new Label("statusline", "<h4>Status</h4><p>Successfully published.</p>").setEscapeModelStrings(false));
        } else if (isNanopubId && SignatureUtils.seemsToHaveSignature(np)) {
            add(StatusLine.createComponent("statusline", ref));
        } else {
            add(new Label("statusline").setVisible(false));
        }
        if (publishedNanopub != null) {
            add(new Label("classes-panel").setVisible(false));
        } else {
            add(ThingListPanel.createComponent("classes-panel", ThingListPanel.Mode.CLASSES, ref, "<em>Searching for classes...</em>"));
        }
        if (isNanopubId) {
            add(new Label("definitions-panel").setVisible(false));
            add(new Label("instances-panel").setVisible(false));
            add(new Label("parts-panel").setVisible(false));
            add(new Label("templates-panel").setVisible(false));
        } else {
            add(ThingListPanel.createComponent("definitions-panel", ThingListPanel.Mode.DESCRIPTIONS, ref, "<em>Searching for term descriptions...</em>"));
            add(ThingListPanel.createComponent("instances-panel", ThingListPanel.Mode.INSTANCES, ref, "<em>Searching for instances...</em>"));
            add(ThingListPanel.createComponent("parts-panel", ThingListPanel.Mode.PARTS, ref, "<em>Searching for parts...</em>"));
            add(ThingListPanel.createComponent("templates-panel", ThingListPanel.Mode.TEMPLATES, ref, "<em>Searching for templates...</em>"));
        }
        add(ExploreDataTable.createComponent("reftable", ref));
    }

    @Override
    protected void onBeforeRender() {
        if (publishedNanopub != null && !getPageParameters().get("postpub-redirect-url").isNull()) {
            String forwardUrl = getPageParameters().get("postpub-redirect-url").toString();
            if (forwardUrl.contains("?")) {
                // TODO: Add here URI of created nanopublication too?
                throw new RedirectToUrlException(forwardUrl);
            } else {
                String paramString = Utils.getPageParametersAsString(new PageParameters().add("id", publishedNanopub.getUri()));
                throw new RedirectToUrlException(forwardUrl + "?" + paramString);
            }
        }
        super.onBeforeRender();
    }

}
