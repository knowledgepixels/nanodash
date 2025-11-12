package com.knowledgepixels.nanodash.component;

import com.github.jsonldjava.shaded.com.google.common.base.Charsets;
import com.knowledgepixels.nanodash.*;
import com.knowledgepixels.nanodash.page.ExplorePage;
import com.knowledgepixels.nanodash.page.NanodashPage;
import com.knowledgepixels.nanodash.template.*;
import org.apache.commons.lang3.Strings;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.nanopub.MalformedNanopubException;
import org.nanopub.Nanopub;
import org.nanopub.NanopubAlreadyFinalizedException;
import org.nanopub.NanopubCreator;
import org.nanopub.extra.security.SignNanopub;
import org.nanopub.extra.security.SignatureAlgorithm;
import org.nanopub.extra.security.TransformContext;
import org.nanopub.extra.server.PublishNanopub;
import org.nanopub.extra.services.APINotReachableException;
import org.nanopub.extra.services.ApiResponse;
import org.nanopub.extra.services.ApiResponseEntry;
import org.nanopub.extra.services.FailedApiCallException;
import org.nanopub.extra.services.NotEnoughAPIInstancesException;
import org.nanopub.extra.services.QueryRef;
import org.nanopub.vocabulary.NPX;
import org.nanopub.vocabulary.NTEMPLATE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.Select2Choice;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Form for publishing a nanopublication.
 */
public class PublishForm extends Panel {

    private static final Logger logger = LoggerFactory.getLogger(PublishForm.class);

    private static final ValueFactory vf = SimpleValueFactory.getInstance();

    private static String creatorPubinfoTemplateId = "https://w3id.org/np/RAukAcWHRDlkqxk7H2XNSegc1WnHI569INvNr-xdptDGI";
    private static String licensePubinfoTempalteId = "https://w3id.org/np/RA0J4vUn_dekg-U1kK3AOEt02p9mT2WO03uGxLDec1jLw";
    private static String defaultProvTemplateId = "https://w3id.org/np/RA7lSq6MuK_TIC6JMSHvLtee3lpLoZDOqLJCLXevnrPoU";
    private static String supersedesPubinfoTemplateId = "https://w3id.org/np/RAoTD7udB2KtUuOuAe74tJi1t3VzK0DyWS7rYVAq1GRvw";
    private static String derivesFromPubinfoTemplateId = "https://w3id.org/np/RARW4MsFkHuwjycNElvEVtuMjpf4yWDL10-0C5l2MqqRQ";

    private static String[] fixedPubInfoTemplates = new String[]{creatorPubinfoTemplateId, licensePubinfoTempalteId};

    /**
     * Fill modes for the nanopublication to be created.
     */
    public enum FillMode {
        /**
         * Use fill mode
         */
        USE,
        /**
         * Supersede fill mode
         */
        SUPERSEDE,
        /**
         * Derive fill mode
         */
        DERIVE
    }

    protected Form<?> form;
    protected FeedbackPanel feedbackPanel;
    private final TemplateContext assertionContext;
    private TemplateContext provenanceContext;
    private List<TemplateContext> pubInfoContexts = new ArrayList<>();
    private Map<String, TemplateContext> pubInfoContextMap = new HashMap<>();
    private List<TemplateContext> requiredPubInfoContexts = new ArrayList<>();
    private String targetNamespace;
    private Class<? extends WebPage> confirmPageClass;

    /**
     * Constructor for the PublishForm.
     *
     * @param id               the Wicket component ID
     * @param pageParams       the parameters for the page, which may include information on how to fill the form
     * @param publishPageClass the class of the page to redirect to after successful publication
     * @param confirmPageClass the class of the confirmation page to show after publication
     */
    public PublishForm(String id, final PageParameters pageParams, Class<? extends WebPage> publishPageClass, Class<? extends WebPage> confirmPageClass) {
        super(id);
        setOutputMarkupId(true);
        this.confirmPageClass = confirmPageClass;

        WebMarkupContainer linkMessageItem = new WebMarkupContainer("link-message-item");
        if (pageParams.get("link-message").isNull()) {
            linkMessageItem.add(new Label("link-message", ""));
            linkMessageItem.setVisible(false);
        } else {
            linkMessageItem.add(new Label("link-message", Utils.sanitizeHtml(pageParams.get("link-message").toString())).setEscapeModelStrings(false));
        }
        add(linkMessageItem);

        final Nanopub fillNp;
        final FillMode fillMode;
        boolean fillOnlyAssertion = false;
        if (!pageParams.get("use").isNull()) {
            fillNp = Utils.getNanopub(pageParams.get("use").toString());
            fillMode = FillMode.USE;
        } else if (!pageParams.get("use-a").isNull()) {
            fillNp = Utils.getNanopub(pageParams.get("use-a").toString());
            fillMode = FillMode.USE;
            fillOnlyAssertion = true;
        } else if (!pageParams.get("supersede").isNull()) {
            fillNp = Utils.getNanopub(pageParams.get("supersede").toString());
            fillMode = FillMode.SUPERSEDE;
            targetNamespace = pageParams.get("supersede").toString().replaceFirst("RA[A-Za-z0-9-_]{43}$", "");
        } else if (!pageParams.get("supersede-a").isNull()) {
            fillNp = Utils.getNanopub(pageParams.get("supersede-a").toString());
            fillMode = FillMode.SUPERSEDE;
            fillOnlyAssertion = true;
        } else if (!pageParams.get("derive").isNull()) {
            fillNp = Utils.getNanopub(pageParams.get("derive").toString());
            fillMode = FillMode.DERIVE;
        } else if (!pageParams.get("derive-a").isNull()) {
            fillNp = Utils.getNanopub(pageParams.get("derive-a").toString());
            fillMode = FillMode.DERIVE;
            fillOnlyAssertion = true;
        } else if (!pageParams.get("fill-all").isNull()) {
            // TODO: This is deprecated and should be removed at some point
            fillNp = Utils.getNanopub(pageParams.get("fill-all").toString());
            fillMode = FillMode.USE;
        } else if (!pageParams.get("fill").isNull()) {
            // TODO: This is deprecated and should be removed at some point
            fillNp = Utils.getNanopub(pageParams.get("fill").toString());
            fillMode = FillMode.SUPERSEDE;
        } else {
            fillNp = null;
            fillMode = null;
        }

        final TemplateData td = TemplateData.get();

        // TODO Properly integrate this namespace feature:
        String templateId = pageParams.get("template").toString();
        if (td.getTemplate(templateId).getTargetNamespace() != null) {
            targetNamespace = td.getTemplate(templateId).getTargetNamespace();
        }
        if (!pageParams.get("target-namespace").isNull()) {
            targetNamespace = pageParams.get("target-namespace").toString();
        }
        if (targetNamespace == null) {
            targetNamespace = Template.DEFAULT_TARGET_NAMESPACE;
        }
        String targetNamespaceLabel = targetNamespace + "...";
        targetNamespace = targetNamespace + "~~~ARTIFACTCODE~~~/";

        assertionContext = new TemplateContext(ContextType.ASSERTION, templateId, "statement", targetNamespace);
        assertionContext.setFillMode(fillMode);
        final String prTemplateId;
        if (pageParams.get("prtemplate").toString() != null) {
            prTemplateId = pageParams.get("prtemplate").toString();
        } else {
            if (fillNp != null && !fillOnlyAssertion) {
                if (td.getProvenanceTemplateId(fillNp) != null) {
                    prTemplateId = td.getProvenanceTemplateId(fillNp).stringValue();
                } else {
                    prTemplateId = "http://purl.org/np/RAcm8OurwUk15WOgBM9wySo-T3a5h6as4K8YR5MBrrxUc";
                }
            } else if (assertionContext.getTemplate().getDefaultProvenance() != null) {
                prTemplateId = assertionContext.getTemplate().getDefaultProvenance().stringValue();
            } else {
                prTemplateId = defaultProvTemplateId;
            }
        }
        provenanceContext = new TemplateContext(ContextType.PROVENANCE, prTemplateId, "pr-statement", targetNamespace);
        for (String t : fixedPubInfoTemplates) {
            // TODO consistently check for latest versions of templates here and below:
            TemplateContext c = new TemplateContext(ContextType.PUBINFO, t, "pi-statement", targetNamespace);
            pubInfoContexts.add(c);
            pubInfoContextMap.put(c.getTemplate().getId(), c);
            requiredPubInfoContexts.add(c);
        }
        if (fillMode == FillMode.SUPERSEDE) {
            TemplateContext c = new TemplateContext(ContextType.PUBINFO, supersedesPubinfoTemplateId, "pi-statement", targetNamespace);
            pubInfoContexts.add(c);
            pubInfoContextMap.put(supersedesPubinfoTemplateId, c);
            //requiredPubInfoContexts.add(c);
            c.setParam("np", fillNp.getUri().stringValue());
        } else if (fillMode == FillMode.DERIVE) {
            TemplateContext c = new TemplateContext(ContextType.PUBINFO, derivesFromPubinfoTemplateId, "pi-statement", targetNamespace);
            pubInfoContexts.add(c);
            pubInfoContextMap.put(derivesFromPubinfoTemplateId, c);
            c.setParam("np", fillNp.getUri().stringValue());
        }
        for (IRI r : assertionContext.getTemplate().getRequiredPubinfoElements()) {
            String latestId = QueryApiAccess.getLatestVersionId(r.stringValue());
            if (pubInfoContextMap.containsKey(r.stringValue()) || pubInfoContextMap.containsKey(latestId)) continue;
            TemplateContext c = new TemplateContext(ContextType.PUBINFO, r.stringValue(), "pi-statement", targetNamespace);
            pubInfoContexts.add(c);
            pubInfoContextMap.put(c.getTemplateId(), c);
            requiredPubInfoContexts.add(c);
        }
        Map<Integer, TemplateContext> piParamIdMap = new HashMap<>();
        for (String k : pageParams.getNamedKeys()) {
            if (!k.matches("pitemplate[1-9][0-9]*")) continue;
            Integer i = Integer.parseInt(k.replaceFirst("^pitemplate([1-9][0-9]*)$", "$1"));
            String tid = pageParams.get(k).toString();
            // TODO Allow for automatically using latest template version:
            //String piTempalteIdLatest = QueryApiAccess.getLatestVersionId(tid);
            TemplateContext c = createPubinfoContext(tid);
            if (piParamIdMap.containsKey(i)) {
                // TODO: handle this error better
                logger.error("ERROR: pitemplate param identifier assigned multiple times: {}", i);
            }
            piParamIdMap.put(i, c);
        }
        if (fillNp != null && !fillOnlyAssertion) {
            for (IRI piTemplateId : td.getPubinfoTemplateIds(fillNp)) {
                String piTempalteIdLatest = QueryApiAccess.getLatestVersionId(piTemplateId.stringValue());
                if (piTempalteIdLatest.equals(supersedesPubinfoTemplateId)) continue;
                if (!pubInfoContextMap.containsKey(piTempalteIdLatest)) {
                    // TODO Allow for automatically using latest template version
                    createPubinfoContext(piTemplateId.stringValue());
                }
            }
        }
        if (!pageParams.get("values-from-query").isEmpty() && !pageParams.get("values-from-query-mapping").isEmpty()) {
            String querySpec = pageParams.get("values-from-query").toString();

            String mapping = pageParams.get("values-from-query-mapping").toString();
            String mapsFrom, mapsTo;
            if (mapping.contains(":")) {
                mapsFrom = mapping.split(":")[0];
                mapsTo = mapping.split(":")[1];
            } else {
                mapsFrom = mapping;
                mapsTo = mapping;
            }
            try {
                ApiResponse resp = QueryApiAccess.get(Utils.parseQueryRef(querySpec));
                int i = 0;
                for (ApiResponseEntry e : resp.getData()) {
                    String mapsToSuffix = "";
                    if (i > 0) mapsToSuffix = "__" + i;
                    assertionContext.setParam(mapsTo + mapsToSuffix, e.get(mapsFrom));
                    i++;
                }
            } catch (FailedApiCallException | APINotReachableException | NotEnoughAPIInstancesException | NullPointerException ex) {
                ex.printStackTrace();
            }
        }
        for (String k : pageParams.getNamedKeys()) {
            if (k.startsWith("param_")) assertionContext.setParam(k.substring(6), pageParams.get(k).toString());
            if (k.startsWith("prparam_")) provenanceContext.setParam(k.substring(8), pageParams.get(k).toString());
            if (k.matches("piparam[1-9][0-9]*_.*")) {
                Integer i = Integer.parseInt(k.replaceFirst("^piparam([1-9][0-9]*)_.*$", "$1"));
                if (!piParamIdMap.containsKey(i)) {
                    // TODO: handle this error better
                    logger.error("ERROR: pitemplate param identifier not found: {}", i);
                    continue;
                }
                String n = k.replaceFirst("^piparam[1-9][0-9]*_(.*)$", "$1");
                logger.info(n);
                piParamIdMap.get(i).setParam(n, pageParams.get(k).toString());
            }
        }

        // Init statements only now, in order to pick up parameter values:
        assertionContext.initStatements();
        provenanceContext.initStatements();
        for (TemplateContext c : pubInfoContexts) {
            c.initStatements();
        }

        String latestAssertionId = QueryApiAccess.getLatestVersionId(assertionContext.getTemplateId());
        if (!assertionContext.getTemplateId().equals(latestAssertionId)) {
            add(new Label("newversion", "There is a new version of this assertion template:"));
            PageParameters params = new PageParameters(pageParams);
            params.set("template", latestAssertionId).remove("formobj");
            add(new BookmarkablePageLink<Void>("newversionlink", publishPageClass, params));
            if ("latest".equals(pageParams.get("template-version").toString())) {
                throw new RestartResponseException(publishPageClass, params);
            }
        } else {
            add(new Label("newversion", "").setVisible(false));
            add(new Label("newversionlink", "").setVisible(false));
        }

        final Nanopub improveNp;
        if (!pageParams.get("improve").isNull()) {
            improveNp = Utils.getNanopub(pageParams.get("improve").toString());
        } else {
            improveNp = null;
        }

        final List<Statement> unusedStatementList = new ArrayList<>();
        final List<Statement> unusedPrStatementList = new ArrayList<>();
        final List<Statement> unusedPiStatementList = new ArrayList<>();
        if (fillNp != null) {
            ValueFiller filler = new ValueFiller(fillNp, ContextType.ASSERTION, true, fillMode);
            filler.fill(assertionContext);
            unusedStatementList.addAll(filler.getUnusedStatements());

            if (!fillOnlyAssertion) {
                ValueFiller prFiller = new ValueFiller(fillNp, ContextType.PROVENANCE, true);
                prFiller.fill(provenanceContext);
                unusedPrStatementList.addAll(prFiller.getUnusedStatements());

                ValueFiller piFiller = new ValueFiller(fillNp, ContextType.PUBINFO, true);
                if (!assertionContext.getTemplate().getTargetNanopubTypes().isEmpty()) {
                    for (Statement st : new ArrayList<>(piFiller.getUnusedStatements())) {
                        if (st.getSubject().stringValue().equals(LocalUri.of("nanopub").stringValue()) && st.getPredicate().equals(NPX.HAS_NANOPUB_TYPE)) {
                            if (assertionContext.getTemplate().getTargetNanopubTypes().contains(st.getObject())) {
                                piFiller.removeUnusedStatement(st);
                            }
                        }
                    }
                }
                for (TemplateContext c : pubInfoContexts) {
                    piFiller.fill(c);
                }
                piFiller.removeUnusedStatements(NanodashSession.get().getUserIri(), FOAF.NAME, null);
                if (piFiller.hasUnusedStatements()) {
                    final String handcodedStatementsTemplateId = "https://w3id.org/np/RAMEgudZsQ1bh1fZhfYnkthqH6YSXpghSE_DEN1I-6eAI";
                    if (!pubInfoContextMap.containsKey(handcodedStatementsTemplateId)) {
                        TemplateContext c = createPubinfoContext(handcodedStatementsTemplateId);
                        c.initStatements();
                        piFiller.fill(c);
                    }
                }
                unusedPiStatementList.addAll(piFiller.getUnusedStatements());
                // TODO: Also use pubinfo templates stated in nanopub to be filled in?
//				Set<IRI> pubinfoTemplateIds = Template.getPubinfoTemplateIds(fillNp);
//				if (!pubinfoTemplateIds.isEmpty()) {
//					ValueFiller piFiller = new ValueFiller(fillNp, ContextType.PUBINFO);
//					for (IRI pubinfoTemplateId : pubinfoTemplateIds) {
//						// TODO: Make smart choice on the ordering in trying to fill in all pubinfo elements
//						piFiller.fill(pubInfoContextMap.get(pubinfoTemplateId.stringValue()));
//					}
//					warningMessage += (piFiller.getWarningMessage() == null ? "" : "Publication info: " + piFiller.getWarningMessage() + " ");
//				}
            }
        } else if (improveNp != null) {
            ValueFiller filler = new ValueFiller(improveNp, ContextType.ASSERTION, true);
            filler.fill(assertionContext);
            unusedStatementList.addAll(filler.getUnusedStatements());
        }
        if (!unusedStatementList.isEmpty()) {
            add(new Label("warnings", "Some content from the existing nanopublication could not be filled in:"));
        } else {
            add(new Label("warnings", "").setVisible(false));
        }
        add(new DataView<Statement>("unused-statements", new ListDataProvider<Statement>(unusedStatementList)) {

            @Override
            protected void populateItem(Item<Statement> item) {
                item.add(new TripleItem("unused-statement", item.getModelObject(), (fillNp != null ? fillNp : improveNp), null));
            }

        });
        add(new DataView<Statement>("unused-prstatements", new ListDataProvider<Statement>(unusedPrStatementList)) {

            @Override
            protected void populateItem(Item<Statement> item) {
                item.add(new TripleItem("unused-prstatement", item.getModelObject(), (fillNp != null ? fillNp : improveNp), null));
            }

        });
        add(new DataView<Statement>("unused-pistatements", new ListDataProvider<Statement>(unusedPiStatementList)) {

            @Override
            protected void populateItem(Item<Statement> item) {
                item.add(new TripleItem("unused-pistatement", item.getModelObject(), (fillNp != null ? fillNp : improveNp), null));
            }

        });

        // Finalize statements, which picks up parameter values in repetitions:
        assertionContext.finalizeStatements();
        provenanceContext.finalizeStatements();
        for (TemplateContext c : pubInfoContexts) {
            c.finalizeStatements();
        }

        final CheckBox consentCheck = new CheckBox("consentcheck", new Model<>(false));
        consentCheck.setRequired(true);
        consentCheck.add(new InvalidityHighlighting());
        consentCheck.add(new IValidator<Boolean>() {

            @Override
            public void validate(IValidatable<Boolean> validatable) {
                if (!Boolean.TRUE.equals(validatable.getValue())) {
                    validatable.error(new ValidationError("You need to check the checkbox that you understand the consequences."));
                }
            }

        });

        form = new Form<Void>("form") {

            @Override
            protected void onConfigure() {
                super.onConfigure();
                form.getFeedbackMessages().clear();
//				formComponents.clear();
            }

            protected void onSubmit() {
                logger.info("Publish form submitted");
                Nanopub signedNp = null;
                try {
                    Nanopub np = createNanopub();
                    logger.info("Nanopublication created: {}", np.getUri());
                    TransformContext tc = new TransformContext(SignatureAlgorithm.RSA, NanodashSession.get().getKeyPair(), NanodashSession.get().getUserIri(), false, false, false);
                    signedNp = SignNanopub.signAndTransform(np, tc);
                    logger.info("Nanopublication signed: {}", signedNp.getUri());
                    String npUrl = PublishNanopub.publish(signedNp);
                    logger.info("Nanopublication published: {}", npUrl);
                } catch (Exception ex) {
                    signedNp = null;
                    logger.error("Nanopublication publishing failed: {}", ex);
                    String message = ex.getClass().getName();
                    if (ex.getMessage() != null) {
                        message = ex.getMessage();
                    }
                    feedbackPanel.error(message);
                }
                if (signedNp != null) {
                    throw new RestartResponseException(getConfirmPage(signedNp, pageParams));
                } else {
                    logger.error("Nanopublication publishing failed");
                }
            }

            @Override
            protected void onValidate() {
                super.onValidate();
                for (Component fc : assertionContext.getComponents()) {
                    processFeedback(fc);
                }
                for (Component fc : provenanceContext.getComponents()) {
                    processFeedback(fc);
                }
                for (TemplateContext c : pubInfoContexts) {
                    for (Component fc : c.getComponents()) {
                        processFeedback(fc);
                    }
                }
            }

            private void processFeedback(Component c) {
                if (c instanceof FormComponent) {
                    ((FormComponent<?>) c).processInput();
                }
                for (FeedbackMessage fm : c.getFeedbackMessages()) {
                    form.getFeedbackMessages().add(fm);
                }
            }

        };
        form.setOutputMarkupId(true);

        form.add(new Label("nanopub-namespace", targetNamespaceLabel));

        form.add(new BookmarkablePageLink<Void>("templatelink", ExplorePage.class, new PageParameters().set("id", assertionContext.getTemplate().getId())));
        form.add(new Label("templatename", assertionContext.getTemplate().getLabel()));
        String description = assertionContext.getTemplate().getLabel();
        if (description == null) description = "";
        form.add(new Label("templatedesc", assertionContext.getTemplate().getDescription()).setEscapeModelStrings(false));

        form.add(new ListView<StatementItem>("statements", assertionContext.getStatementItems()) {

            protected void populateItem(ListItem<StatementItem> item) {
                item.add(item.getModelObject());
            }

        });

        final Map<String, Boolean> handledProvTemplates = new HashMap<>();
        final String defaultProvTemplateId;
        if (assertionContext.getTemplate().getDefaultProvenance() != null) {
            defaultProvTemplateId = assertionContext.getTemplate().getDefaultProvenance().stringValue();
            handledProvTemplates.put(defaultProvTemplateId, true);
        } else {
            defaultProvTemplateId = null;
        }
        final List<String> recommendedProvTemplateOptionIds = new ArrayList<>();
        final List<String> provTemplateOptionIds = new ArrayList<>();
        if (pageParams.get("prtemplate-options").isNull()) {
            // TODO Make this dynamic and consider updated templates:
            recommendedProvTemplateOptionIds.add("https://w3id.org/np/RA7lSq6MuK_TIC6JMSHvLtee3lpLoZDOqLJCLXevnrPoU");
            recommendedProvTemplateOptionIds.add("http://purl.org/np/RAcTpoh5Ra0ssqmcpOgWdaZ_YiPE6demO6cpw-2RvSNs8");
            recommendedProvTemplateOptionIds.add("http://purl.org/np/RA4LGtuOqTIMqVAkjnfBXk1YDcAPNadP5CGiaJiBkdHCQ");
            recommendedProvTemplateOptionIds.add("http://purl.org/np/RAl_-VTw9Re_uRF8r8y0rjlfnu7FlhTa8xg_8xkcweqiE");
            recommendedProvTemplateOptionIds.add("https://w3id.org/np/RASORV2mMEVpS4lWh2bwUTEcV-RWjbD9RPbN7J0PIeYAU");
            recommendedProvTemplateOptionIds.add("http://purl.org/np/RAjkBbM5yQm7hKH1l_Jk3HAUqWi3Bd57TPmAOZCsZmi_M");
            recommendedProvTemplateOptionIds.add("http://purl.org/np/RAGXx_k9eQMnXaCbsXMsJbGClwZtQEGNg0GVJu6amdAVw");
            recommendedProvTemplateOptionIds.add("http://purl.org/np/RA1fnITI3Pu1UQ0CHghNpys3JwQrM32LBnjmDLoayp9-4");
            recommendedProvTemplateOptionIds.add("http://purl.org/np/RAJgbsGeGdTG-zq_gU0TLw4s3raMgoRk-mPlc2DSLXvE0");
            recommendedProvTemplateOptionIds.add("http://purl.org/np/RA6SXfhUY-xeblZU8HhPddw6tsu-C5NXevG6C_zv4bMxU");
            for (String s : recommendedProvTemplateOptionIds) {
                handledProvTemplates.put(s, true);
            }

            for (ApiResponseEntry t : td.getProvenanceTemplates()) {
                String tid = t.get("np");
                if (handledProvTemplates.containsKey(tid)) continue;
                provTemplateOptionIds.add(tid);
                handledProvTemplates.put(tid, true);
            }
        } else {
            for (String s : pageParams.get("prtemplate-options").toString().split(" ")) {
                if (handledProvTemplates.containsKey(s)) continue;
                recommendedProvTemplateOptionIds.add(s);
                handledProvTemplates.put(s, true);
            }
        }

        ChoiceProvider<String> prTemplateChoiceProvider = new ChoiceProvider<String>() {

            @Override
            public String getDisplayValue(String object) {
                if (object == null || object.isEmpty()) return "";
                Template t = td.getTemplate(object);
                if (t != null) return t.getLabel();
                return object;
            }

            @Override
            public String getIdValue(String object) {
                return object;
            }

            // Getting strange errors with Tomcat if this method is not overridden:
            @Override
            public void detach() {
            }

            @Override
            public void query(String term, int page, Response<String> response) {
                if (term == null) term = "";
                term = term.toLowerCase();
                if (pageParams.get("prtemplate").toString() != null) {
                    // Using this work-around with "——" because 'optgroup' is not available through Wicket's Select2 classes
                    response.add("—— default for this link ——");
                    response.add(prTemplateId);
                }
                if (defaultProvTemplateId != null) {
                    response.add("—— default for this template ——");
                    response.add(defaultProvTemplateId);
                }
                if (!recommendedProvTemplateOptionIds.isEmpty()) {
                    if (pageParams.get("prtemplate-options").isNull()) {
                        response.add("—— recommended ——");
                    }
                    for (String s : recommendedProvTemplateOptionIds) {
                        if (s.toLowerCase().contains(term) || getDisplayValue(s).toLowerCase().contains(term)) {
                            response.add(s);
                        }
                    }
                }
                if (!provTemplateOptionIds.isEmpty()) {
                    response.add("—— others ——");
                    for (String s : provTemplateOptionIds) {
                        if (s.toLowerCase().contains(term) || getDisplayValue(s).toLowerCase().contains(term)) {
                            response.add(s);
                        }
                    }
                }
            }

            @Override
            public Collection<String> toChoices(Collection<String> ids) {
                return ids;
            }

        };
        final IModel<String> prTemplateModel = Model.of(provenanceContext.getTemplate().getId());
        Select2Choice<String> prTemplateChoice = new Select2Choice<String>("prtemplate", prTemplateModel, prTemplateChoiceProvider);
        prTemplateChoice.setRequired(true);
        prTemplateChoice.getSettings().setCloseOnSelect(true);
        prTemplateChoice.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                String o = prTemplateModel.getObject();
                if (o.startsWith("——")) {
                    o = provenanceContext.getTemplate().getId();
                    prTemplateModel.setObject(o);
                }
                provenanceContext = new TemplateContext(ContextType.PROVENANCE, prTemplateModel.getObject(), "pr-statement", targetNamespace);
                provenanceContext.initStatements();
                refreshProvenance(target);
                provenanceContext.finalizeStatements();
            }

        });
        form.add(prTemplateChoice);
        refreshProvenance(null);

        final Map<String, Boolean> handledPiTemplates = new HashMap<>();
        final List<String> recommendedPiTemplateOptionIds = new ArrayList<>();
        final List<String> piTemplateOptionIds = new ArrayList<>();
        // TODO Make this dynamic and consider updated templates:
        recommendedPiTemplateOptionIds.add("http://purl.org/np/RAXflINqt3smqxV5Aq7E9lzje4uLdkKIOefa6Bp8oJ8CY");
        recommendedPiTemplateOptionIds.add("https://w3id.org/np/RARW4MsFkHuwjycNElvEVtuMjpf4yWDL10-0C5l2MqqRQ");
        recommendedPiTemplateOptionIds.add("https://w3id.org/np/RA16U9Wo30ObhrK1NzH7EsmVRiRtvEuEA_Dfc-u8WkUCA");
        recommendedPiTemplateOptionIds.add("http://purl.org/np/RAdyqI6k07V5nAS82C6hvIDtNWk179EIV4DV-sLbOFKg4");
        recommendedPiTemplateOptionIds.add("https://w3id.org/np/RAjvEpLZUE7rMoa8q6mWSsN6utJDp-5FmgO47YGsbgw3w");
        recommendedPiTemplateOptionIds.add("http://purl.org/np/RAxuGRKID6yNg63V5Mf0ot2NjncOnodh-mkN3qT_1txGI");
        for (TemplateContext c : pubInfoContexts) {
            String s = c.getTemplate().getId();
            handledPiTemplates.put(s, true);
        }
        for (String s : recommendedPiTemplateOptionIds) {
            handledPiTemplates.put(s, true);
        }

        for (ApiResponseEntry entry : td.getPubInfoTemplates()) {
            String tid = entry.get("np");
            if (handledPiTemplates.containsKey(tid)) continue;
            piTemplateOptionIds.add(tid);
            handledPiTemplates.put(tid, true);
        }

        ChoiceProvider<String> piTemplateChoiceProvider = new ChoiceProvider<String>() {

            @Override
            public String getDisplayValue(String object) {
                if (object == null || object.isEmpty()) return "";
                Template t = td.getTemplate(object);
                if (t != null) return t.getLabel();
                return object;
            }

            @Override
            public String getIdValue(String object) {
                return object;
            }

            // Getting strange errors with Tomcat if this method is not overridden:
            @Override
            public void detach() {
            }

            @Override
            public void query(String term, int page, Response<String> response) {
                if (term == null) term = "";
                term = term.toLowerCase();
                if (!recommendedPiTemplateOptionIds.isEmpty()) {
                    response.add("—— recommended ——");
                    for (String s : recommendedPiTemplateOptionIds) {
                        boolean isAlreadyUsed = false;
                        for (TemplateContext c : pubInfoContexts) {
                            // TODO: make this more efficient/nicer
                            if (c.getTemplate().getId().equals(s)) {
                                isAlreadyUsed = true;
                                break;
                            }
                        }
                        if (isAlreadyUsed) continue;
                        if (s.toLowerCase().contains(term) || getDisplayValue(s).toLowerCase().contains(term)) {
                            response.add(s);
                        }
                    }
                }
                if (!piTemplateOptionIds.isEmpty()) {
                    response.add("—— others ——");
                    for (String s : piTemplateOptionIds) {
                        boolean isAlreadyUsed = false;
                        for (TemplateContext c : pubInfoContexts) {
                            // TODO: make this more efficient/nicer
                            if (c.getTemplate().getId().equals(s)) {
                                isAlreadyUsed = true;
                                break;
                            }
                        }
                        if (isAlreadyUsed) continue;
                        if (s.toLowerCase().contains(term) || getDisplayValue(s).toLowerCase().contains(term)) {
                            response.add(s);
                        }
                    }
                }
            }

            @Override
            public Collection<String> toChoices(Collection<String> ids) {
                return ids;
            }

        };
        final IModel<String> newPiTemplateModel = Model.of();
        Select2Choice<String> piTemplateChoice = new Select2Choice<String>("pitemplate", newPiTemplateModel, piTemplateChoiceProvider);
        piTemplateChoice.getSettings().setCloseOnSelect(true);
        piTemplateChoice.getSettings().setPlaceholder("add element...");
        piTemplateChoice.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (newPiTemplateModel.getObject().startsWith("——")) {
                    newPiTemplateModel.setObject(null);
                    refreshPubInfo(target);
                    return;
                }
                String id = newPiTemplateModel.getObject();
                TemplateContext c = new TemplateContext(ContextType.PUBINFO, id, "pi-statement", targetNamespace);
                c.initStatements();
                pubInfoContexts.add(c);
                newPiTemplateModel.setObject(null);
                refreshPubInfo(target);
                c.finalizeStatements();
            }

        });
        form.add(piTemplateChoice);
        refreshPubInfo(null);

        form.add(consentCheck);
        add(form);

        feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);
    }

    private void refreshProvenance(AjaxRequestTarget target) {
        if (target != null) {
            form.remove("prtemplatelink");
            form.remove("pr-statements");
            target.add(form);
            target.appendJavaScript("updateElements();");
        }
        form.add(new BookmarkablePageLink<Void>("prtemplatelink", ExplorePage.class, new PageParameters().set("id", provenanceContext.getTemplate().getId())));
        ListView<StatementItem> list = new ListView<StatementItem>("pr-statements", provenanceContext.getStatementItems()) {

            protected void populateItem(ListItem<StatementItem> item) {
                item.add(item.getModelObject());
            }

        };
        list.setOutputMarkupId(true);
        form.add(list);
    }

    private void refreshPubInfo(AjaxRequestTarget target) {
        ListView<TemplateContext> list = new ListView<TemplateContext>("pis", pubInfoContexts) {

            protected void populateItem(ListItem<TemplateContext> item) {
                final TemplateContext pic = item.getModelObject();
                item.add(new Label("pitemplatename", pic.getTemplate().getLabel()));
                item.add(new BookmarkablePageLink<Void>("pitemplatelink", ExplorePage.class, new PageParameters().set("id", pic.getTemplate().getId())));
                Label remove = new Label("piremove", "×");
                item.add(remove);
                remove.add(new AjaxEventBehavior("click") {

                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        pubInfoContexts.remove(pic);
                        target.add(PublishForm.this);
                        target.appendJavaScript("updateElements();");
                    }

                });
                if (requiredPubInfoContexts.contains(pic)) remove.setVisible(false);
                item.add(new ListView<StatementItem>("pi-statements", pic.getStatementItems()) {

                    protected void populateItem(ListItem<StatementItem> item) {
                        item.add(item.getModelObject());
                    }

                });
            }

        };
        list.setOutputMarkupId(true);
        if (target == null) {
            form.add(list);
        } else {
            form.remove("pis");
            form.add(list);
            target.add(form);
            target.appendJavaScript("updateElements();");
        }
    }

    private TemplateContext createPubinfoContext(String piTemplateId) {
        TemplateContext c;
        if (pubInfoContextMap.containsKey(piTemplateId)) {
            c = pubInfoContextMap.get(piTemplateId);
        } else {
            c = new TemplateContext(ContextType.PUBINFO, piTemplateId, "pi-statement", targetNamespace);
            pubInfoContextMap.put(piTemplateId, c);
            pubInfoContexts.add(c);
        }
        return c;
    }

    private synchronized Nanopub createNanopub() throws MalformedNanopubException, NanopubAlreadyFinalizedException {
        assertionContext.getIntroducedIris().clear();
        NanopubCreator npCreator = new NanopubCreator(targetNamespace);
        npCreator.setAssertionUri(vf.createIRI(targetNamespace + "assertion"));
        assertionContext.propagateStatements(npCreator);
        provenanceContext.propagateStatements(npCreator);
        for (TemplateContext c : pubInfoContexts) {
            c.propagateStatements(npCreator);
        }
        for (IRI introducedIri : assertionContext.getIntroducedIris()) {
            npCreator.addPubinfoStatement(NPX.INTRODUCES, introducedIri);
        }
        for (IRI embeddedIri : assertionContext.getEmbeddedIris()) {
            npCreator.addPubinfoStatement(NPX.EMBEDS, embeddedIri);
        }
        npCreator.addNamespace("this", targetNamespace);
        npCreator.addNamespace("sub", targetNamespace + "/");
        npCreator.addTimestampNow();
        IRI templateUri = assertionContext.getTemplate().getNanopub().getUri();
        npCreator.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_TEMPLATE, templateUri);
        IRI prTemplateUri = provenanceContext.getTemplate().getNanopub().getUri();
        npCreator.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_PROVENANCE_TEMPLATE, prTemplateUri);
        for (TemplateContext c : pubInfoContexts) {
            IRI piTemplateUri = c.getTemplate().getNanopub().getUri();
            npCreator.addPubinfoStatement(NTEMPLATE.WAS_CREATED_FROM_PUBINFO_TEMPLATE, piTemplateUri);
        }
        String nanopubLabel = getNanopubLabel(npCreator);
        if (nanopubLabel != null) {
            npCreator.addPubinfoStatement(RDFS.LABEL, vf.createLiteral(nanopubLabel));
        }
        for (IRI type : assertionContext.getTemplate().getTargetNanopubTypes()) {
            npCreator.addPubinfoStatement(NPX.HAS_NANOPUB_TYPE, type);
        }
        IRI userIri = NanodashSession.get().getUserIri();
        if (User.getName(userIri) != null) {
            npCreator.addPubinfoStatement(userIri, FOAF.NAME, vf.createLiteral(User.getName(userIri)));
            npCreator.addNamespace("foaf", FOAF.NAMESPACE);
        }
        String websiteUrl = NanodashPreferences.get().getWebsiteUrl();
        if (websiteUrl != null) {
            npCreator.addPubinfoStatement(NPX.WAS_CREATED_AT, vf.createIRI(websiteUrl));
        }
        return npCreator.finalizeNanopub();
    }

    private String getNanopubLabel(NanopubCreator npCreator) {
        if (assertionContext.getTemplate().getNanopubLabelPattern() == null) return null;

        Map<IRI, String> labelMap = new HashMap<>();
        for (Statement st : npCreator.getCurrentPubinfoStatements()) {
            if (st.getPredicate().equals(RDFS.LABEL) && st.getObject() instanceof Literal objL) {
                labelMap.put((IRI) st.getSubject(), objL.stringValue());
            }
        }

        String nanopubLabel = assertionContext.getTemplate().getNanopubLabelPattern();
        while (nanopubLabel.matches(".*\\$\\{[_a-zA-Z0-9-]+\\}.*")) {
            String placeholderPostfix = nanopubLabel.replaceFirst("^.*\\$\\{([_a-zA-Z0-9-]+)\\}.*$", "$1");
            IRI placeholderIriHash = vf.createIRI(assertionContext.getTemplateId() + "#" + placeholderPostfix);
            IRI placeholderIriSlash = vf.createIRI(assertionContext.getTemplateId() + "/" + placeholderPostfix);
            IRI placeholderIri = null;
            String placeholderValue = "";
            if (assertionContext.getComponentModels().get(placeholderIriSlash) != null) {
                placeholderIri = placeholderIriSlash;
            } else {
                placeholderIri = placeholderIriHash;
            }
            IModel<String> m = (IModel<String>) assertionContext.getComponentModels().get(placeholderIri);
            if (m != null) placeholderValue = m.orElse("").getObject();
            if (placeholderValue == null) placeholderValue = "";
            String placeholderLabel = placeholderValue;
            if (assertionContext.getTemplate().isUriPlaceholder(placeholderIri)) {
                try {
                    // TODO Fix this. It doesn't work for placeholders with auto-encode placeholders, etc.
                    //      Not sure we need labels for these, but this code should be improved anyway.
                    String prefix = assertionContext.getTemplate().getPrefix(placeholderIri);
                    if (prefix != null) placeholderValue = prefix + placeholderValue;
                    IRI placeholderValueIri = vf.createIRI(placeholderValue);
                    String l = assertionContext.getTemplate().getLabel(placeholderValueIri);
                    if (labelMap.containsKey(placeholderValueIri)) {
                        l = labelMap.get(placeholderValueIri);
                    }
                    if (l == null) l = GuidedChoiceItem.getLabel(placeholderValue);
                    if (assertionContext.getTemplate().isAgentPlaceholder(placeholderIri) && !placeholderValue.isEmpty()) {
                        l = User.getName(vf.createIRI(placeholderValue));
                    }
                    if (l != null && !l.isEmpty()) {
                        placeholderLabel = l.replaceFirst(" - .*$", "");
                    } else {
                        placeholderLabel = Utils.getShortNameFromURI(placeholderValueIri);
                    }
                } catch (Exception ex) {
                    logger.error("Nanopub label placeholder IRI error: {}", ex.getMessage());
                }
            }
            placeholderLabel = placeholderLabel.replaceAll("\\s+", " ");
            if (placeholderLabel.length() > 100) placeholderLabel = placeholderLabel.substring(0, 97) + "...";
            nanopubLabel = Strings.CS.replace(nanopubLabel, "${" + placeholderPostfix + "}", placeholderLabel);
        }
        return nanopubLabel;
    }

    private NanodashPage getConfirmPage(Nanopub signedNp, PageParameters pageParams) {
        try {
            return (NanodashPage) confirmPageClass.getConstructor(Nanopub.class, PageParameters.class).newInstance(signedNp, pageParams);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException |
                 InvocationTargetException | NoSuchMethodException | SecurityException ex) {
            logger.error("Could not create instance of confirmation page: {}", ex.getMessage());
        }
        return null;
    }

    /**
     * Returns a hint whether the form is stateless or not.
     *
     * @return false if the form is stateful, true if it is stateless.
     */
    // This is supposed to solve the problem that sometimes (but only sometimes) form content is reset
    // if the user browses other pages in parallel:
    protected boolean getStatelessHint() {
        return false;
    }

}
