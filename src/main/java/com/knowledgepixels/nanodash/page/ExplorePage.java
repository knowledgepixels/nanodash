package com.knowledgepixels.nanodash.page;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.component.*;
import com.knowledgepixels.nanodash.domain.AbstractResourceWithProfile;
import com.knowledgepixels.nanodash.domain.IndividualAgent;
import com.knowledgepixels.nanodash.domain.User;
import com.knowledgepixels.nanodash.repository.MaintainedResourceRepository;
import com.knowledgepixels.nanodash.repository.SpaceRepository;
import jakarta.servlet.http.HttpServletRequest;
import net.trustyuri.TrustyUriUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.INamedParameters.NamedPair;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.commonjava.mimeparse.MIMEParse;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.nanopub.Nanopub;
import org.nanopub.NanopubUtils;
import org.nanopub.extra.security.SignatureUtils;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.vocabulary.KPXL_GRLC;
import org.nanopub.vocabulary.NTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * ExplorePage is a page that allows users to explore a specific Nanopublication or Thing.
 */
public class ExplorePage extends NanodashPage {

    private static final Logger logger = LoggerFactory.getLogger(ExplorePage.class);

    /**
     * The mount path for this page.
     */
    public static final String MOUNT_PATH = "/explore";

    private static final String DESCRIBED_IN_VIEW = "https://w3id.org/np/RAMH_7qMY-jmgXr2jqqk5F_XW7t2k2n3NCB6LtoKEXDzY/described-in-view";
    private static final String CLASSES_VIEW = "https://w3id.org/np/RAHPtR1VriEW09tcvZhrM8Dr3vE1JnMWWi9-ajKJWNOJs/classes-view";
    private static final String INSTANCES_VIEW = "https://w3id.org/np/RABXfsNoT_RYlk8LpDmKfJ2poSlvIGk3jgq4DkR4YLAps/instances-view";
    private static final String TEMPLATES_VIEW = "https://w3id.org/np/RAP0-S9PUUVF1rQiqo8vq8z6XWsXkeGBUo60DJf8JsXsc/templates-view";

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
        // Flag the just-published nanopub so the uniform "successfully published"
        // message (and the "missing introduction" hint) render in the title bar; see
        // JustPublishedMessagePanel.
        super(parameters.set("id", publishedNanopub.getUri()).set("just-published", publishedNanopub.getUri().stringValue()));
        this.publishedNanopub = publishedNanopub;

        // The success confirmation now lives in the title bar. The "publish another
        // with same template" buttons below are kept here for possible later
        // reactivation, but are not currently shown.
        WebMarkupContainer publishConfirmPanel = new WebMarkupContainer("publish-confirm-panel");
        publishConfirmPanel.setVisible(false);
        WebMarkupContainer missingIntroWarning = new WebMarkupContainer("missing-intro-warning");
        missingIntroWarning.add(new ExternalLink("profile-link", "."));
        publishConfirmPanel.add(missingIntroWarning);

        PageParameters plainLinkParams = new PageParameters();
        plainLinkParams.set("template", parameters.get("template"));
        if (!parameters.get("template-version").isEmpty()) {
            plainLinkParams.set("template-version", parameters.get("template-version"));
        }
        publishConfirmPanel.add(new BookmarkablePageLink<Void>("publish-another-link", PublishPage.class, plainLinkParams));

        PageParameters linkParams = new PageParameters(parameters);
        linkParams.remove("supersede");
        linkParams.remove("supersede-a");
        linkParams.remove("just-published");
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

        String tempRef = parameters.get("id").toString();
        // Sometimes these Wicket session IDs end up here and they can mess up the query cache:
        tempRef = tempRef.replaceFirst(";jsessionid.*$", "");

        String contextId = parameters.get("context").toString("");

        // Known resources are explored via their own page's Explore tab; forward
        // there instead of rendering the generic explore page.
        if (publishedNanopub == null) {
            if (SpaceRepository.get().findById(tempRef) != null) {
                throw new RestartResponseException(SpacePage.class, new PageParameters().set("id", tempRef).set("tab", "explore"));
            } else if (MaintainedResourceRepository.get().findById(tempRef) != null) {
                throw new RestartResponseException(MaintainedResourcePage.class, new PageParameters().set("id", tempRef).set("tab", "explore"));
            } else if (User.getUserData().isUser(tempRef)) {
                throw new RestartResponseException(UserPage.class, new PageParameters().set("id", tempRef).set("tab", "explore"));
            }
            // Note: forwarding to the part page when the context is a maintained
            // resource is handled further down (after np resolution), gated on
            // actual part membership rather than the context type alone.
        }

        ResourceTabs.Tab activeTab = ResourceTabs.activeFromParam(parameters);
        TitleBar titleBar = new TitleBar("titlebar", this, null);
        add(titleBar);

        if (SpaceRepository.get().findById(contextId) != null) {
            add(new BookmarkablePageLink<Void>("back-to-context-link", SpacePage.class, new PageParameters().set("id", contextId)).setBody(LoadableDetachableModel.of(() -> {
                var space = SpaceRepository.get().findById(contextId);
                return "back to " + (space == null ? contextId : space.getLabel());
            })));
        } else if (MaintainedResourceRepository.get().findById(contextId) != null) {
            add(new BookmarkablePageLink<Void>("back-to-context-link", MaintainedResourcePage.class, new PageParameters().set("id", contextId)).setBody(LoadableDetachableModel.of(() -> {
                var resource = MaintainedResourceRepository.get().findById(contextId);
                return "back to " + (resource == null ? contextId : resource.getLabel());
            })));
        } else if (IndividualAgent.isUser(contextId)) {
            add(new BookmarkablePageLink<Void>("back-to-context-link", UserPage.class, new PageParameters().set("id", contextId)).setBody(LoadableDetachableModel.of(() -> "back to " + User.getShortDisplayName(Utils.vf.createIRI(contextId)))));
        } else {
            add(new Label("back-to-context-link").setVisible(false));
        }

        if (User.getUserData().isUser(tempRef)) {
            add(new BookmarkablePageLink<Void>("to-specific-page-link", UserPage.class, parameters).setBody(Model.of("go to user page")));
        } else if (SpaceRepository.get().findById(tempRef) != null) {
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
                ApiResponse resp = ApiCache.retrieveResponseSync(new QueryRef(QueryApiAccess.GET_LATEST_THING_NANOPUB, params), false);
                if (!resp.getData().isEmpty()) {
                    // TODO We take the most recent in case more than one latest version exists. Make other latest versions visible too.
                    npId = resp.getData().getFirst().get("latestVersion");
                }
                np = Utils.getAsNanopub(npId);
            }
        }

        // Forward to the part page when the explored resource actually qualifies as a
        // part of the context: either explicitly requested via forward-to-part, or
        // whenever the context is a maintained resource. In both cases the membership
        // is verified below (dct:isPartOf / skos:inScheme or view-display applicability)
        // rather than assumed from the context type.
        boolean contextIsMaintainedResource = !contextId.isEmpty() && MaintainedResourceRepository.get().findById(contextId) != null;
        if ((parameters.get("forward-to-part").toString("").equals("true") || contextIsMaintainedResource) && !contextId.isEmpty() && publishedNanopub == null) {
            parameters.remove("forward-to-part");
            Set<IRI> classes = new HashSet<>();
            if (np != null) {
                Set<String> introducedIds = NanopubUtils.getIntroducedIriIds(np);
                if (introducedIds.size() == 1 && introducedIds.iterator().next().equals(tempRef)) {
                    for (Statement st : np.getAssertion()) {
                        if (!st.getSubject().stringValue().equals(tempRef)) continue;
                        if (st.getPredicate().equals(DCTERMS.IS_PART_OF) || st.getPredicate().equals(SKOS.IN_SCHEME)) {
                            String resourceId = st.getObject().stringValue();
                            if (MaintainedResourceRepository.get().findById(resourceId) == null) continue;
                            throw new RestartResponseException(ResourcePartPage.class, parameters);
                        } else if (st.getPredicate().equals(RDF.TYPE) && st.getObject() instanceof IRI objIri) {
                            classes.add(objIri);
                        }
                    }
                }
            }
            AbstractResourceWithProfile contextResource = AbstractResourceWithProfile.get(contextId);
            if (contextResource instanceof IndividualAgent && !IndividualAgent.isUser(contextId)) {
                contextResource = null;
            }
            if (contextResource != null && contextResource.appliesTo(tempRef, classes)) {
                throw new RestartResponseException(ResourcePartPage.class, parameters);
            }
        }

        WebMarkupContainer nanopubSection = new WebMarkupContainer("nanopub-section");

        if (np == null) {
            nanopubSection.setVisible(false);
            nanopubSection.add(new Label("nanopub-header", ""));
            nanopubSection.add(new Label("nanopub", ""));
            raw.setVisible(false);
            nanopubSection.add(raw);
            nanopubSection.add(new WebMarkupContainer("use-template").setVisible(false));
            nanopubSection.add(new WebMarkupContainer("run-query").setVisible(false));
        } else {

            // Check whether we should redirect to Nanopub Registry for machine-friendly formats:
            String mimeType = Utils.TYPE_HTML;
            try {
                HttpServletRequest httpRequest = (HttpServletRequest) getRequest().getContainerRequest();
                mimeType = MIMEParse.bestMatch(Utils.SUPPORTED_TYPES_LIST, httpRequest.getHeader("Accept"));
            } catch (Exception ex) {
                logger.info("Error determining MIME type from Accept header.", ex);
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
                nanopubHeaderLabel = String.format(nanopubHeaderLabel, "📌 Nanopublication");
            } else {
                nanopubHeaderLabel = String.format(nanopubHeaderLabel, "🚩 Minted in Nanopublication");
            }
            nanopubSection.add(new Label("nanopub-header", nanopubHeaderLabel).setEscapeModelStrings(false));
            nanopubSection.add(new NanopubItem("nanopub", NanopubElement.get(np)));
            String url = Utils.getMainRegistryUrl() + "np/" + TrustyUriUtils.getArtifactCode(np.getUri().stringValue());
            raw.add(new ExternalLink("trig-txt", url + ".trig.txt"));
            raw.add(new ExternalLink("jsonld-txt", url + ".jsonld.txt"));
            raw.add(new ExternalLink("nq-txt", url + ".nq.txt"));
            raw.add(new ExternalLink("xml-txt", url + ".xml.txt"));
            raw.add(new ExternalLink("trig", url + ".trig"));
            raw.add(new ExternalLink("jsonld", url + ".jsonld"));
            raw.add(new ExternalLink("nq", url + ".nq"));
            raw.add(new ExternalLink("xml", url + ".xml"));
            String npUri = np.getUri().stringValue();
            raw.add(createAssertionLink("a-turtle", npUri, "turtle", false));
            raw.add(createAssertionLink("a-turtle-txt", npUri, "turtle", true));
            raw.add(createAssertionLink("a-jsonld", npUri, "jsonld", false));
            raw.add(createAssertionLink("a-jsonld-txt", npUri, "jsonld", true));
            raw.add(createAssertionLink("a-nt", npUri, "nt", false));
            raw.add(createAssertionLink("a-nt-txt", npUri, "nt", true));
            raw.add(createAssertionLink("a-rdfxml", npUri, "rdfxml", false));
            raw.add(createAssertionLink("a-rdfxml-txt", npUri, "rdfxml", true));
            nanopubSection.add(raw);
            if (Utils.isNanopubOfClass(np, NTEMPLATE.ASSERTION_TEMPLATE)) {
                nanopubSection.add(new WebMarkupContainer("use-template").add(new BookmarkablePageLink<Void>("template-link", PublishPage.class, new PageParameters().set("template", np.getUri()))));
            } else {
                nanopubSection.add(new WebMarkupContainer("use-template").setVisible(false));
            }
            if (Utils.isNanopubOfClass(np, KPXL_GRLC.GRLC_QUERY)) {
                nanopubSection.add(new WebMarkupContainer("run-query").add(new BookmarkablePageLink<Void>("query-link", QueryPage.class, new PageParameters().set("id", np.getUri()))));
            } else {
                nanopubSection.add(new WebMarkupContainer("run-query").setVisible(false));
            }
        }
        add(nanopubSection);

        final String ref = tempRef;
        final String shortName;
        if (publishedNanopub != null) {
            shortName = NanopubUtils.getLabel(np);
        } else if (parameters.get("label").isEmpty()) {
            shortName = Utils.getShortNameFromURI(ref);
        } else {
            shortName = parameters.get("label").toString();
        }
        add(new Label("pagetitle", shortName + " (explore) | nanodash"));
        add(new Label("termname", shortName));

        //add(new ExternalLink("urilink", ref, ref));
        add(new ExternalLinkWithActionsPanel("urilink", Model.of(ref)));

        if (publishedNanopub != null) {
            add(new Label("statusLine").setVisible(false));
        } else if (np != null && SignatureUtils.seemsToHaveSignature(np)) {
            add(StatusLine.createComponent("statusLine", np.getUri().stringValue()));
        } else {
            add(new Label("statusLine").setVisible(false));
        }
        WebMarkupContainer infoSection = new WebMarkupContainer("info-section");
        if (publishedNanopub != null) {
            infoSection.add(new Label("classes-panel").setVisible(false));
        } else {
            View classesView = View.get(CLASSES_VIEW);
            QueryRef classesQueryRef = new QueryRef(classesView.getQuery().getQueryId(), "thing", ref);
            infoSection.add(QueryResultListBuilder.create("classes-panel", classesQueryRef, new ViewDisplay(classesView)).build());
        }
        if (isNanopubId) {
            infoSection.add(new Label("definitions-panel").setVisible(false));
            infoSection.add(new Label("instances-panel").setVisible(false));
            infoSection.add(new Label("templates-panel").setVisible(false));
        } else {
            View describedInView = View.get(DESCRIBED_IN_VIEW);
            QueryRef describedInQueryRef = new QueryRef(describedInView.getQuery().getQueryId(), "term", ref);
            infoSection.add(QueryResultNanopubSetBuilder.create("definitions-panel", describedInQueryRef, new ViewDisplay(describedInView)).build());

            View instancesView = View.get(INSTANCES_VIEW);
            QueryRef instancesQueryRef = new QueryRef(instancesView.getQuery().getQueryId(), "class", ref);
            infoSection.add(QueryResultListBuilder.create("instances-panel", instancesQueryRef, new ViewDisplay(instancesView)).build());

            View templatesView = View.get(TEMPLATES_VIEW);
            QueryRef templatesQueryRef = new QueryRef(templatesView.getQuery().getQueryId(), "thing", ref);
            infoSection.add(QueryResultListBuilder.create("templates-panel", templatesQueryRef, new ViewDisplay(templatesView)).build());
        }
        add(infoSection);

        // References to the explored thing (the References view, moved here from
        // the About pages).
        WebMarkupContainer referencesSection = new WebMarkupContainer("references-section");
        View refView = View.get(ReferencesPage.REFERENCES_VIEW);
        QueryRef refQueryRef = new QueryRef(refView.getQuery().getQueryId(), "ref", ref);
        referencesSection.add(QueryResultTableBuilder.create("references", refQueryRef, new ViewDisplay(refView)).build());
        add(referencesSection);

        // Tab layout: the Content tab shows the nanopublication itself (for
        // nanopub / minted-in-nanopub things) or, for plain terms, the info
        // section (Assigned to, Described in, instances, templates). The Explore
        // tab shows only References (for all cases).
        titleBar.setTabs(new ResourceTabs("tabs", exploreTabParams(ref, contextId, parameters), activeTab));
        boolean contentTab = (activeTab == ResourceTabs.Tab.CONTENT);
        boolean hasNanopub = (np != null);
        nanopubSection.setVisible(contentTab && hasNanopub);
        infoSection.setVisible(publishedNanopub == null && contentTab && !hasNanopub);
        referencesSection.setVisible(!contentTab);
    }

    /**
     * Builds the parameter set carried across the Content/Explore tabs: the
     * resolved thing id, plus the optional context and label, so tab switches
     * stay on the same resolved thing without dragging publish-only parameters.
     */
    private static PageParameters exploreTabParams(String ref, String contextId, PageParameters parameters) {
        PageParameters p = new PageParameters().set("id", ref);
        if (!contextId.isEmpty()) p.set("context", contextId);
        if (!parameters.get("label").isEmpty()) p.set("label", parameters.get("label").toString());
        return p;
    }

    @Override
    protected void onBeforeRender() {
        if (publishedNanopub != null && !getPageParameters().get("postpub-redirect-url").isNull()) {
            String forwardUrl = getPageParameters().get("postpub-redirect-url").toString();
            if (forwardUrl.contains("?")) {
                // TODO: Add here URI of created nanopublication too?
                throw new RedirectToUrlException(forwardUrl);
            } else {
                Set<String> introducedIds = NanopubUtils.getIntroducedIriIds(publishedNanopub);
                String redirectId = introducedIds.size() == 1 ? introducedIds.iterator().next() : publishedNanopub.getUri().stringValue();
                String paramString = Utils.getPageParametersAsString(new PageParameters().set("id", redirectId));
                throw new RedirectToUrlException(forwardUrl + "?" + paramString);
            }
        }
        super.onBeforeRender();
    }

    private BookmarkablePageLink<Void> createAssertionLink(String wicketId, String npUri, String format, boolean txt) {
        PageParameters params = new PageParameters()
                .set("type", "np")
                .set("id", npUri)
                .set("format", format)
                .set("assertions", "");
        if (txt) {
            params.set("txt", "");
        }
        return new BookmarkablePageLink<>(wicketId, DownloadRdfPage.class, params);
    }

}
