package com.knowledgepixels.nanodash.component;

import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.action.NanopubAction;
import com.knowledgepixels.nanodash.page.TypePage;
import com.knowledgepixels.nanodash.page.UserPage;
import com.knowledgepixels.nanodash.template.*;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.nanopub.SimpleCreatorPattern;
import org.nanopub.extra.security.MalformedCryptoElementException;
import org.nanopub.extra.security.SignatureUtils;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * A panel that displays a nanopublication with its header, footer, assertion, provenance, and pubinfo.
 */
public class NanopubItem extends Panel {

    private static final long serialVersionUID = -5109507637942030910L;

    /**
     * Date format for displaying the creation date and time of the nanopub.
     */
    public static SimpleDateFormat simpleDateTimeFormat = new SimpleDateFormat("d MMM yyyy, HH:mm:ss zzz");
    /**
     * Date format for displaying the creation time of the nanopub without time.
     */
    public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMM yyyy");

    private boolean isInitialized = false;
    private NanopubElement n;
    private boolean hideAssertion = false;
    private boolean hideProvenance = false;
    private boolean hidePubinfo = false;
    private boolean hideHeader = false;
    private boolean hideFooter = false;
    private boolean hideActionMenu = false;
    private List<NanopubAction> actions;
    private IRI signerId;
    private String tempalteId;

    /**
     * Creates a NanopubItem panel.
     *
     * @param id         the Wicket component ID
     * @param n          the NanopubElement to display
     * @param tempalteId the ID of the template to use for rendering the assertion.
     */
    public NanopubItem(String id, NanopubElement n, String tempalteId) {
        super(id);
        this.n = n;
        this.tempalteId = tempalteId;
    }

    /**
     * Creates a NanopubItem panel with a default template ID.
     *
     * @param id the Wicket component ID
     * @param n  the NanopubElement to display
     */
    public NanopubItem(String id, NanopubElement n) {
        this(id, n, null);
    }

    private void initialize() {
        if (isInitialized) return;

        String pubkey = n.getPubkey();
        signerId = n.getSignerId();

        if (hideHeader) {
            add(new Label("header", "").setVisible(false));
        } else {
            WebMarkupContainer header = new WebMarkupContainer("header");
            String labelString = n.getLabel();
            if (labelString == null || labelString.isBlank()) labelString = Utils.getShortNanopubId(n.getUri());
            header.add(NanodashLink.createLink("nanopub-id-link", n.getUri(), labelString));
            if (!hideActionMenu && (actions == null || !actions.isEmpty())) {
                NanodashSession session = NanodashSession.get();
                final boolean isOwnNanopub = (session.getUserIri() != null && session.getUserIri().equals(User.getUserData().getUserIri(pubkey, false))) ||
                                             ((pubkey != null) && pubkey.equals(session.getPubkeyString()));
                final boolean hasLocalPubkey = session.getUserIri() != null && session.getPubkeyString() != null && session.getPubkeyString().equals(pubkey);
                final List<NanopubAction> actionList = new ArrayList<>();
                final Map<String, NanopubAction> actionMap = new HashMap<>();
                List<NanopubAction> allActions = new ArrayList<>();
                if (actions == null) {
                    allActions.addAll(Arrays.asList(NanopubAction.defaultActions));
                    allActions.addAll(NanopubAction.getActionsFromPreferences(NanodashPreferences.get()));
                } else {
                    allActions.addAll(actions);
                }
                for (NanopubAction action : allActions) {
                    if (isOwnNanopub && !action.isApplicableToOwnNanopubs()) continue;
                    if (isOwnNanopub && !hasLocalPubkey && !action.isApplicableToOthersNanopubs() && !Utils.hasNanodashLocation(pubkey))
                        continue;
                    if (!isOwnNanopub && !action.isApplicableToOthersNanopubs()) continue;
                    if (!action.isApplicableTo(n.getNanopub())) continue;
                    actionList.add(action);
                    actionMap.put(action.getLinkLabel(n.getNanopub()), action);
                }
                header.add(new ActionMenu("action-menu", actionList, n));
            } else {
                header.add(new Label("action-menu", "").setVisible(false));
            }
            header.add(new DataView<IRI>("typespan", new ListDataProvider<IRI>(Utils.getTypes(n.getNanopub()))) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(Item<IRI> item) {
                    IRI typeIri = item.getModelObject();
                    String label = Utils.getTypeLabel(typeIri);
                    item.add(new BookmarkablePageLink<Void>("type", TypePage.class, new PageParameters().add("id", typeIri)).setBody(Model.of(label)));
                }

            });
            add(header);
        }

        if (hideFooter) {
            add(new Label("footer", "").setVisible(false));
        } else {
            WebMarkupContainer footer = new WebMarkupContainer("footer");
            if (n.getCreationTime() != null) {
                footer.add(new Label("datetime", simpleDateTimeFormat.format(n.getCreationTime().getTime())));
            } else {
                footer.add(new Label("datetime", "(undated)"));
            }

            List<IRI> authors = SimpleCreatorPattern.getAuthorList(n.getNanopub());
            WebMarkupContainer authorsSpan = new WebMarkupContainer("authors-span");
            if (authors.isEmpty()) {
                authorsSpan.setVisible(false);
                footer.add(new Label("creator-post", "").setVisible(false));
            } else {
                IRI mainAuthor = authors.get(0);
                BookmarkablePageLink<Void> mainAuthorLink = new BookmarkablePageLink<Void>("main-author-link", UserPage.class, new PageParameters().add("id", mainAuthor));
                String authorName = n.getFoafNameMap().get(mainAuthor.stringValue());
                if (authorName == null) {
                    authorName = User.getShortDisplayName(mainAuthor);
                }
                mainAuthorLink.add(new Label("main-author-text", authorName));
                authorsSpan.add(mainAuthorLink);
                if (authors.size() > 1) {
                    authorsSpan.add(new Label("authors-post", " et al. (authors)"));
                } else {
                    authorsSpan.add(new Label("authors-post", " (author)"));
                }
                footer.add(new Label("creator-post", " (creator)"));
            }
            footer.add(authorsSpan);

            PageParameters params = new PageParameters();

            // ----------
            // TODO Clean this up and move to helper method:
            IRI uIri = User.findSingleIdForPubkey(pubkey);
            if (uIri == null) {
                try {
                    Set<IRI> signers = SignatureUtils.getSignatureElement(n.getNanopub()).getSigners();
                    if (signers.size() == 1) {
                        uIri = signers.iterator().next();
                    } else {
                        Set<IRI> creators = SimpleCreatorPattern.getCreators(n.getNanopub());
                        if (creators.size() == 1) uIri = creators.iterator().next();
                    }
                } catch (MalformedCryptoElementException ex) {
                }
            }
            // ----------

            if (uIri != null) params.add("id", uIri);
            BookmarkablePageLink<Void> userLink = new BookmarkablePageLink<Void>("creator-link", UserPage.class, params);
            String userString;
            if (signerId != null) {
                userString = User.getShortDisplayName(signerId, pubkey);
            } else {
                userString = User.getShortDisplayName(uIri, pubkey);
            }
            userLink.add(new Label("creator-text", userString));
            footer.add(userLink);

            String positiveNotes = "";
            String negativeNotes = "";
            if (n.seemsToHaveSignature()) {
                try {
                    if (!n.hasValidSignature()) {
                        negativeNotes = "- invalid signature";
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    negativeNotes = "- malformed or legacy signature";
                }
            }
            footer.add(new Label("positive-notes", positiveNotes));
            footer.add(new Label("negative-notes", negativeNotes));
            add(footer);
        }

        final TemplateData td = TemplateData.get();

        WebMarkupContainer assertion = new WebMarkupContainer("assertion");

        if (hideAssertion) {
            assertion.setVisible(false);
        } else {
            Template assertionTemplate = td.getTemplate(n.getNanopub());
            if (tempalteId != null) assertionTemplate = td.getTemplate(tempalteId);
            if (assertionTemplate == null)
                assertionTemplate = td.getTemplate("http://purl.org/np/RAFu2BNmgHrjOTJ8SKRnKaRp-VP8AOOb7xX88ob0DZRsU");
            List<StatementItem> assertionStatements = new ArrayList<>();
            ValueFiller assertionFiller = new ValueFiller(n.getNanopub(), ContextType.ASSERTION, false);
            TemplateContext context = new TemplateContext(ContextType.ASSERTION, assertionTemplate.getId(), "assertion-statement", n.getNanopub());
            populateStatementItemList(context, assertionFiller, assertionStatements);

            assertion.add(createStatementView("assertion-statements", assertionStatements));
            assertion.add(new DataView<Statement>("unused-assertion-statements", new ListDataProvider<Statement>(assertionFiller.getUnusedStatements())) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(Item<Statement> item) {
                    item.add(new TripleItem("unused-assertion-statement", item.getModelObject(), n.getNanopub(), null));
                }

            });
        }
        add(assertion);

        WebMarkupContainer provenance = new WebMarkupContainer("provenance");
        if (hideProvenance) {
            provenance.setVisible(false);
        } else {
            Template provenanceTemplate = td.getProvenanceTemplate(n.getNanopub());
            if (provenanceTemplate == null)
                provenanceTemplate = td.getTemplate("http://purl.org/np/RA3Jxq5JJjluUNEpiMtxbiIHa7Yt-w8f9FiyexEstD5R4");
            List<StatementItem> provenanceStatements = new ArrayList<>();
            ValueFiller provenanceFiller = new ValueFiller(n.getNanopub(), ContextType.PROVENANCE, false);
            TemplateContext prContext = new TemplateContext(ContextType.PROVENANCE, provenanceTemplate.getId(), "provenance-statement", n.getNanopub());
            populateStatementItemList(prContext, provenanceFiller, provenanceStatements);
            provenance.add(createStatementView("provenance-statements", provenanceStatements));
            provenance.add(new DataView<Statement>("unused-provenance-statements", new ListDataProvider<Statement>(provenanceFiller.getUnusedStatements())) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(Item<Statement> item) {
                    item.add(new TripleItem("unused-provenance-statement", item.getModelObject(), n.getNanopub(), null));
                }

            });
        }
        add(provenance);

        WebMarkupContainer pubInfo = new WebMarkupContainer("pubinfo");
        if (hidePubinfo) {
            pubInfo.setVisible(false);
        } else {
            ValueFiller pubinfoFiller = new ValueFiller(n.getNanopub(), ContextType.PUBINFO, false);

            // TODO We should do this better:
            List<String> pubinfoAuthorTemplateIds = new ArrayList<>();
            List<String> pubinfoTemplateIds = new ArrayList<>();
            for (IRI iri : td.getPubinfoTemplateIds(n.getNanopub())) {
                if (iri.stringValue().equals("https://w3id.org/np/RA16U9Wo30ObhrK1NzH7EsmVRiRtvEuEA_Dfc-u8WkUCA")) { // author list
                    pubinfoAuthorTemplateIds.add(iri.stringValue());
                } else if (iri.stringValue().equals("http://purl.org/np/RA4vTctL3Luaj8oI_sPiN7I_8xEnR_hkdz5gN7bCvZpNY")) { // authors
                    pubinfoAuthorTemplateIds.add(iri.stringValue());
                } else if (iri.stringValue().equals("http://purl.org/np/RAA2MfqdBCzmz9yVWjKLXNbyfBNcwsMmOqcNUxkk1maIM")) { // creator
                    pubinfoTemplateIds.add(0, iri.stringValue());
                } else {
                    pubinfoTemplateIds.add(iri.stringValue());
                }
            }
            pubinfoTemplateIds.addAll(0, pubinfoAuthorTemplateIds);
            pubinfoTemplateIds.add("https://w3id.org/np/RAXVsr624oEAJvCt1WZXoUJ90lFYC5LUMoYHgEUOwmrLw"); // user name
            pubinfoTemplateIds.add("https://w3id.org/np/RARJj78P72NR5edKOnu_f4ePE9NYYuW2m2pM-fEoobMBk"); // nanopub label
            pubinfoTemplateIds.add("https://w3id.org/np/RA8iXbwvOC7BwVHuvAhFV235j2582SyAYJ2sfov19ZOlg"); // nanopub type
            pubinfoTemplateIds.add("https://w3id.org/np/RALmzqHlrRfeTD8ESZdKFyDNYY6eFuyQ8GAe_4N5eVytc"); // timestamp
            pubinfoTemplateIds.add("https://w3id.org/np/RADVnztsdSc36ffAXTxiIdXpYEMLiJrRENqaJ2Qn2LX3Y"); // templates
            pubinfoTemplateIds.add("https://w3id.org/np/RAvqXPNKPf56b2226oqhKzARyvIhnpnTrRpLGC1cYweMw"); // introductions
            pubinfoTemplateIds.add("https://w3id.org/np/RAgIlomuR39mN-Z39bbv59-h2DgQBnLyNdL22YmOJ_VHM"); // labels from APIs
            pubinfoTemplateIds.add("https://w3id.org/np/RAoWx0AJvNw-WqkGgZO4k8udNCg6kMcGZARN3DgO_5TII"); // contributor names
            pubinfoTemplateIds.add("https://w3id.org/np/RAY_M7GUmyOTjXQbJArzhVVzQ5XvgHt0JR7h2LZo6TXvY"); // signature
            pubinfoTemplateIds.add("https://w3id.org/np/RA_TZ9tvF6sBewmbIGbTFguLOPUUS70huklacisZrYtYw"); // creation site
            pubinfoTemplateIds.add("https://w3id.org/np/RAE-zsHxw2VoE6emhSY_Fkr5p_li5Qb8FrREqUwdWdzyM"); // generic

            List<TemplateContext> contexts = new ArrayList<>();
            List<TemplateContext> genericContexts = new ArrayList<>();
            for (String s : pubinfoTemplateIds) {
                TemplateContext piContext = new TemplateContext(ContextType.PUBINFO, s, "pubinfo-statement", n.getNanopub());
                if (piContext.willMatchAnyTriple()) {
                    genericContexts.add(piContext);
                } else if (piContext.getTemplateId().equals("https://w3id.org/np/RAE-zsHxw2VoE6emhSY_Fkr5p_li5Qb8FrREqUwdWdzyM")) {
                    // TODO: This is a work-around; check why this template doesn't give true to willMatchAnyTriple()
                    genericContexts.add(piContext);
                } else {
                    contexts.add(piContext);
                }
            }
            contexts.addAll(genericContexts);  // make sure the generic one are at the end
            List<WebMarkupContainer> elements = new ArrayList<>();
            for (TemplateContext piContext : contexts) {
                WebMarkupContainer pubInfoElement = new WebMarkupContainer("pubinfo-element");
                List<StatementItem> pubinfoStatements = new ArrayList<>();
                populateStatementItemList(piContext, pubinfoFiller, pubinfoStatements);
                if (!pubinfoStatements.isEmpty()) {
                    pubInfoElement.add(createStatementView("pubinfo-statements", pubinfoStatements));
                    elements.add(pubInfoElement);
                }
            }
            pubInfo.add(new DataView<WebMarkupContainer>("pubinfo-elements", new ListDataProvider<WebMarkupContainer>(elements)) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(Item<WebMarkupContainer> item) {
                    item.add(item.getModelObject());
                }

            });
        }
        add(pubInfo);

        isInitialized = true;
    }

    /**
     * Hides the provenance part of the nanopub item.
     *
     * @return this NanopubItem instance for method chaining
     */
    public NanopubItem hideProvenance() {
        if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
        hideProvenance = true;
        return this;
    }

    /**
     * Sets whether the provenance part of the nanopub item should be hidden.
     *
     * @param hideProvenance true to hide provenance, false to show it
     * @return this NanopubItem instance for method chaining
     */
    public NanopubItem setProvenanceHidden(boolean hideProvenance) {
        if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
        this.hideProvenance = hideProvenance;
        return this;
    }

    /**
     * Hides the pubinfo part of the nanopub item.
     *
     * @return this NanopubItem instance for method chaining
     */
    public NanopubItem hidePubinfo() {
        if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
        hidePubinfo = true;
        return this;
    }

    /**
     * Sets whether the pubinfo part of the nanopub item should be hidden.
     *
     * @param hidePubinfo true to hide pubinfo, false to show it
     * @return this NanopubItem instance for method chaining
     */
    public NanopubItem setPubinfoHidden(boolean hidePubinfo) {
        if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
        this.hidePubinfo = hidePubinfo;
        return this;
    }

    /**
     * Hides the header part of the nanopub item.
     *
     * @return this NanopubItem instance for method chaining
     */
    public NanopubItem hideHeader() {
        if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
        hideHeader = true;
        return this;
    }

    /**
     * Sets whether the header part of the nanopub item should be hidden.
     *
     * @param hideHeader true to hide header, false to show it
     * @return this NanopubItem instance for method chaining
     */
    public NanopubItem setHeaderHidden(boolean hideHeader) {
        if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
        this.hideHeader = hideHeader;
        return this;
    }

    /**
     * Hides the footer part of the nanopub item.
     *
     * @return this NanopubItem instance for method chaining
     */
    public NanopubItem hideFooter() {
        if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
        hideFooter = true;
        return this;
    }

    /**
     * Sets whether the footer part of the nanopub item should be hidden.
     *
     * @param hideFooter true to hide footer, false to show it
     * @return this NanopubItem instance for method chaining
     */
    public NanopubItem setFooterHidden(boolean hideFooter) {
        if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
        this.hideFooter = hideFooter;
        return this;
    }

    /**
     * Sets the nanopub item to a minimal state, hiding assertion, action menu, provenance, and pubinfo.
     *
     * @return this NanopubItem instance for method chaining
     */
    public NanopubItem setMinimal() {
        if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
        this.hideAssertion = true;
        this.hideActionMenu = true;
        setProvenanceHidden(true).setPubinfoHidden(true);
        return this;
    }

    /**
     * Adds the given actions to the nanopub item.
     *
     * @param a a {@link com.knowledgepixels.nanodash.action.NanopubAction} object
     * @return this NanopubItem instance for method chaining
     */
    public NanopubItem addActions(NanopubAction... a) {
        if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
        if (actions == null) actions = new ArrayList<>();
        for (NanopubAction na : a) {
            actions.add(na);
        }
        return this;
    }

    /**
     * Sets the nanopub item to have no actions.
     *
     * @return this NanopubItem instance for method chaining
     */
    public NanopubItem noActions() {
        if (isInitialized) throw new RuntimeException("Nanopub item is already initialized");
        actions = new ArrayList<>();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onBeforeRender() {
        initialize();
        super.onBeforeRender();
    }

    private void populateStatementItemList(TemplateContext context, ValueFiller filler, List<StatementItem> list) {
        context.initStatements();
        if (signerId != null) {
            context.getComponentModels().put(Template.CREATOR_PLACEHOLDER, Model.of(signerId.stringValue()));
        }
        filler.fill(context);
        for (StatementItem si : context.getStatementItems()) {
            if (si.isMatched()) {
                list.add(si);
            }
        }
    }

    private DataView<StatementItem> createStatementView(String elementId, List<StatementItem> list) {
        return new DataView<StatementItem>(elementId, new ListDataProvider<StatementItem>(list)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<StatementItem> item) {
                item.add(item.getModelObject());
            }

        };
    }

}
